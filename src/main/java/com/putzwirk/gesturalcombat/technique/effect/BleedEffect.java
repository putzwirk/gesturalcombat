package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "gesturalcombat")
public final class BleedEffect implements TechniqueEffect {
    private static final Map<UUID, Bleed> BLEEDS = new HashMap<>();
    private static final int PULSE_TICKS = 20;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final int bleedDamage;
    private final int bleedTicks;

    public BleedEffect(double range, double arcDegrees, float damageMultiplier, int bleedDamage, int bleedTicks) {
        this(range, arcDegrees, (double) damageMultiplier, bleedDamage, bleedTicks);
    }

    public BleedEffect(double range, double arcDegrees, double damageMultiplier, int bleedDamage, int bleedTicks) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.bleedDamage = Math.max(1, bleedDamage);
        this.bleedTicks = Math.max(PULSE_TICKS, bleedTicks);
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier));
        long now = level.getGameTime();
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, damage, technique, AbilityConfigs.knockback(technique, stack, 0.2), 0.0);
            BLEEDS.put(target.getUUID(), new Bleed(now + bleedTicks, player.getUUID(), bleedDamage, now + PULSE_TICKS));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || BLEEDS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Bleed>> iterator = BLEEDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Bleed> entry = iterator.next();
            Bleed bleed = entry.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(bleed.owner());
            Entity target = findEntity(server, entry.getKey());
            if (owner == null || target == null || !target.isAlive() || target.isRemoved()) {
                iterator.remove();
                continue;
            }
            long now = owner.serverLevel().getGameTime();
            if (now >= bleed.expiry()) {
                iterator.remove();
                continue;
            }
            if (now < bleed.nextTick()) {
                continue;
            }
            entry.setValue(new Bleed(bleed.expiry(), bleed.owner(), bleed.damage(), now + PULSE_TICKS));
            target.invulnerableTime = 0;
            target.hurt(owner.damageSources().playerAttack(owner), bleed.damage());
            if (target.level() instanceof ServerLevel serverLevel) {
                Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.0f, 0.0f), 0.9f),
                        at.x, at.y, at.z, 5, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    private static Entity findEntity(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private record Bleed(long expiry, UUID owner, float damage, long nextTick) {
    }
}
