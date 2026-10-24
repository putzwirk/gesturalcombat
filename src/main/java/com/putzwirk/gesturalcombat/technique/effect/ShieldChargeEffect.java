package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.DashMovement;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.HitStun;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.TechniqueImmunity;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ShieldChargeEffect implements TechniqueEffect {
    private static final double CONTACT_DISTANCE = 1.35;
    private static final double FREE_DASH_DISTANCE = 2.4;
    private static final int MOVE_TICKS = 3;

    private final int stunTicks;

    public ShieldChargeEffect(int stunTicks) {
        this.stunTicks = Math.max(1, stunTicks);
    }

    @Override
    public int windupTicks() {
        return 3;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        int windup = AbilityConfigs.windupTicks(technique, stack);
        TechniqueImmunity.grant(player, windup + 12);
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, Hitbox.arc(3.2, 110.0));
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, 1.0));

        LivingEntity target = EffectSupport.targetInFront(player, hitbox.range() + 2.0, 75.0);
        Vec3 destination;
        if (target != null) {
            destination = DashMovement.stopPoint(player, target, CONTACT_DISTANCE + 0.3, hitbox.range());
        } else {
            destination = DashMovement.forwardPoint(player, Math.min(FREE_DASH_DISTANCE, hitbox.range()));
        }
        int ticks = DashMovement.run(player, level, destination, MOVE_TICKS, windup + 2);
        EffectSupport.sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.4f);

        GestureScheduler.schedule(level, ticks + windup, () -> {
            if (!player.isAlive() || player.isRemoved() || player.level() != level) {
                return;
            }
            double force = AbilityConfigs.knockback(technique, stack, 0.0);
            double lift = AbilityConfigs.lift(technique, stack, 0.0);
            LivingEntity closest = null;
            for (LivingEntity hit : EffectSupport.targets(player, level, hitbox, target)) {
                EffectSupport.hit(player, hit, damage, technique, force, lift, true);
                HitStun.pin(hit, stunTicks, true);
                if (closest == null) {
                    closest = hit;
                }
            }
            EffectSupport.slash(level, player, hitbox, technique, stack);
            EffectSupport.sound(level, player, SoundEvents.SHIELD_BLOCK, 1.0f, 0.8f);
            double recoil = AbilityConfigs.selfKnockback(technique, stack, 0.45);
            if (closest != null && recoil > 0.0) {
                Vec3 back = awayFrom(player, closest, recoil);
                GestureScheduler.schedule(level, 3, () -> {
                    if (player.isAlive() && !player.isRemoved() && player.level() == level) {
                        player.push(back.x, 0.02, back.z);
                        player.hurtMarked = true;
                    }
                });
            }
        });
    }

    private static Vec3 awayFrom(ServerPlayer player, LivingEntity target, double strength) {
        Vec3 delta = player.position().subtract(target.position());
        Vec3 flat = new Vec3(delta.x, 0.0, delta.z);
        if (flat.lengthSqr() > 1.0E-4) {
            return flat.normalize().scale(strength);
        }
        Vec3 look = player.getLookAngle();
        Vec3 fallback = new Vec3(look.x, 0.0, look.z);
        return fallback.lengthSqr() > 1.0E-4 ? fallback.normalize().scale(-strength) : Vec3.ZERO;
    }
}
