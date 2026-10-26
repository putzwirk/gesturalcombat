package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Assassin {
    private static final double BACKSTAB_DEGREES = 100.0;
    private static final float CRIT_CHANCE = 0.15f;
    private static final float CRIT_MULTIPLIER = 1.6f;

    private Assassin() {
    }

    public static double strike(ServerPlayer player, LivingEntity target, double damage, boolean finisher) {
        double result = damage;
        if (isBackstab(player, target)) {
            result *= 1.35;
        }
        if (player.getRandom().nextFloat() < CRIT_CHANCE) {
            result *= CRIT_MULTIPLIER;
        }
        int debt = States.debt(player);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60,
                debt >= 6 ? 1 : 0, false, false, false));
        if (finisher) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, false));
        }
        return result;
    }

    private static boolean isBackstab(ServerPlayer player, LivingEntity target) {
        Vec3 toPlayer = player.position().subtract(target.position());
        Vec3 flat = new Vec3(toPlayer.x, 0.0, toPlayer.z);
        if (flat.lengthSqr() < 1.0e-4) {
            return false;
        }
        Vec3 look = target.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0, look.z);
        if (facing.lengthSqr() < 1.0e-4) {
            return false;
        }
        double dot = facing.normalize().dot(flat.normalize());
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot)))) > BACKSTAB_DEGREES;
    }
}
