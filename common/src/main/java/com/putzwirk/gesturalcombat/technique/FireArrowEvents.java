package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FireArrowEvents {
    private static final int PATCH_TICKS = 120;
    private static final double PATCH_RADIUS = 2.5;
    private static final int IGNITE_SECONDS = 4;

    private FireArrowEvents() {
    }

        public static boolean impact(AbstractArrow arrow, HitResult result) {
        if (!arrow.getTags().contains(FirePatchManager.TAG)) {
            return false;
        }
        if (!(arrow.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 at = arrow.position();
        if (result instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            at = new Vec3(target.getX(), target.getY() + 0.05, target.getZ());
            Entity owner = arrow.getOwner();
            if (owner instanceof LivingEntity shooter) {
                target.hurt(level.damageSources().arrow(arrow, shooter), (float) arrow.getBaseDamage());
            }
            target.igniteForSeconds(IGNITE_SECONDS);
        } else if (result != null) {
            at = result.getLocation();
        }
        try {
            FirePatchManager.spawn(level, at, PATCH_TICKS, PATCH_RADIUS);
        } catch (RuntimeException exception) {
            Gesturalcombat.LOGGER.error("Failed to spawn fire patch", exception);
        }
        arrow.discard();
        return true;
    }
}
