package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.server.GestureCastService;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.weapon.DualWield;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID)
public final class PairedGuard {
    private static final int SWORD_TICKS = 20;
    private static final int SPEAR_TICKS = 16;
    private static final int AXE_TICKS = 12;
    private static final int AXE_STACKS = 3;
    private static final double AXE_RADIUS = 3.5;
    private static final double AXE_PER_STACK = 0.55;
    private static final double SPEAR_LUNGE = 0.85;
    private static final double SPEAR_RANGE = 4.2;
    private static final double SPEAR_DAMAGE = 0.85;

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    private PairedGuard() {
    }

    private static final class Active {
        final Archetype archetype;
        final Technique technique;
        final ItemStack stack;
        final long until;
        int stacks;

        Active(Archetype archetype, Technique technique, ItemStack stack, long until) {
            this.archetype = archetype;
            this.technique = technique;
            this.stack = stack;
            this.until = until;
        }
    }

    public static boolean begin(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        if (!DualWield.paired(player)) {
            return false;
        }
        Archetype archetype = Archetype.of(stack);
        int ticks = switch (archetype) {
            case SWORD -> SWORD_TICKS;
            case SPEAR -> SPEAR_TICKS;
            case AXE -> AXE_TICKS;
            default -> 0;
        };
        if (ticks <= 0) {
            return false;
        }
        prune(level);
        Active active = new Active(archetype, technique, stack, level.getGameTime() + ticks);
        ACTIVE.put(player.getUUID(), active);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS, 1.0f, archetype == Archetype.SWORD ? 1.35f : 0.8f);
        GestureScheduler.schedule(level, ticks, () -> expire(player, active));
        return true;
    }

    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        Active active = ACTIVE.get(player.getUUID());
        if (active == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now > active.until) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        event.setCanceled(true);
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9f, 1.1f);
        switch (active.archetype) {
            case AXE -> {
                active.stacks++;
                if (active.stacks >= AXE_STACKS) {
                    ACTIVE.remove(player.getUUID());
                    discharge(player, active);
                }
            }
            case SPEAR -> {
                ACTIVE.remove(player.getUUID());
                riposte(player, active, event.getSource().getEntity());
            }
            default -> {
            }
        }
    }

    private static void expire(ServerPlayer player, Active active) {
        Active current = ACTIVE.get(player.getUUID());
        if (current != active) {
            return;
        }
        ACTIVE.remove(player.getUUID());
        if (!player.isAlive() || player.isRemoved()) {
            return;
        }
        if (active.archetype == Archetype.AXE && active.stacks > 0) {
            discharge(player, active);
        }
    }

    private static void discharge(ServerPlayer player, Active active) {
        Level level = player.level();
        double base = EffectSupport.damageOf(player, active.technique, active.stack);
        for (LivingEntity target : Hitbox.sphere(AXE_RADIUS).query(player, level)) {
            EffectSupport.hit(player, target, (float) (base * AXE_PER_STACK * active.stacks),
                    active.technique, 0.35, 0.0);
            States.addSunder(target, 1);
        }
        Fx.ring(player, active.technique, active.stack, player.position().add(0.0, 0.5, 0.0), AXE_RADIUS);
        EffectSupport.sound(level, player, SoundEvents.GENERIC_EXPLODE, 0.5f, 0.8f);
        EffectSupport.swingSound(level, player, active.technique);
    }

    private static void riposte(ServerPlayer player, Active active, Entity attacker) {
        Level level = player.level();
        Vec3 direction = player.getLookAngle();
        if (attacker != null) {
            Vec3 to = attacker.position().subtract(player.position());
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (flat.lengthSqr() > 1.0e-4) {
                direction = flat.normalize();
                float yaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
                player.setYRot(yaw);
                player.setYHeadRot(yaw);
                player.setYBodyRot(yaw);
            }
        }
        Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
        if (flat.lengthSqr() > 1.0e-4) {
            flat = flat.normalize();
            player.setDeltaMovement(flat.x * SPEAR_LUNGE, 0.1, flat.z * SPEAR_LUNGE);
            player.hurtMarked = true;
        }
        Hitbox pierce = Hitbox.box(SPEAR_RANGE, 0.9, 1.8).withLineOfSight(true);
        double base = EffectSupport.damageOf(player, active.technique, active.stack);
        for (LivingEntity target : pierce.query(player, level)) {
            EffectSupport.hit(player, target, (float) (base * SPEAR_DAMAGE), active.technique, 0.35, 0.0);
            States.addBleed(target, player, 1);
        }
        Fx.ring(player, active.technique, active.stack, player.position().add(0.0, 0.6, 0.0), SPEAR_RANGE * 0.6);
        EffectSupport.sound(level, player, SoundEvents.TRIDENT_RIPTIDE_3, 0.8f, 1.0f);
    }

    private static void prune(Level level) {
        if (ACTIVE.size() < 96) {
            return;
        }
        long now = level.getGameTime();
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().until < now);
    }
}
