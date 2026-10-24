package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

public final class DashMovement {
    private static final double STEP = 0.55;
    private static final double MIN_STEP = 0.12;
    private static final Set<RelativeMovement> KEEP_ROTATION = EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT);

    private DashMovement() {
    }

    public static Vec3 stopPoint(ServerPlayer player, LivingEntity target, double stopDistance, double maxDistance) {
        Vec3 from = player.position();
        Vec3 delta = new Vec3(target.getX() - from.x, 0.0, target.getZ() - from.z);
        double distance = delta.length();
        if (distance < 1.0E-3) {
            return from;
        }
        double radius = Math.max(0.3, target.getBbWidth() * 0.5);
        double travel = Math.min(maxDistance, Math.max(0.0, distance - radius - stopDistance));
        return from.add(delta.scale(travel / distance));
    }

    public static Vec3 forwardPoint(ServerPlayer player, double distance) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return player.position();
        }
        return player.position().add(flat.normalize().scale(distance));
    }

    public static int run(ServerPlayer player, Level level, Vec3 destination, int maxTicks, int holdTicks) {
        Vec3 from = player.position();
        Vec3 end = clamp(player, level, from, destination);
        int hold = Math.max(1, holdTicks);
        double distance = horizontal(from, end);
        if (distance < MIN_STEP) {
            for (int i = 1; i <= hold; i++) {
                GestureScheduler.schedule(level, i, () -> step(player, level, from, false));
            }
            return 0;
        }
        int ticks = Math.max(1, Math.min(maxTicks, (int) Math.ceil(distance / STEP)));
        for (int i = 1; i <= ticks; i++) {
            float progress = i / (float) ticks;
            Vec3 point = from.add(end.subtract(from).scale(progress));
            GestureScheduler.schedule(level, i, () -> step(player, level, point, true));
        }
        for (int i = 1; i <= hold; i++) {
            GestureScheduler.schedule(level, ticks + i, () -> step(player, level, end, false));
        }
        return ticks;
    }

    public static void hold(ServerPlayer player, Level level, int ticks) {
        Vec3 at = player.position();
        for (int i = 1; i <= Math.max(1, ticks); i++) {
            GestureScheduler.schedule(level, i, () -> step(player, level, at, false));
        }
    }

    private static Vec3 clamp(ServerPlayer player, Level level, Vec3 from, Vec3 destination) {
        Vec3 delta = new Vec3(destination.x - from.x, 0.0, destination.z - from.z);
        while (horizontal(from, from.add(delta)) > MIN_STEP) {
            Vec3 candidate = from.add(delta);
            if (level.noCollision(player, player.getBoundingBox().move(candidate.x - from.x, 0.0, candidate.z - from.z))) {
                return candidate;
            }
            delta = delta.scale(0.6);
        }
        return from;
    }

    private static void step(ServerPlayer player, Level level, Vec3 point, boolean trail) {
        if (!player.isAlive() || player.isRemoved() || player.level() != level) {
            return;
        }
        double dx = point.x - player.getX();
        double dz = point.z - player.getZ();
        if (!level.noCollision(player, player.getBoundingBox().move(dx, 0.0, dz))) {
            return;
        }
        player.connection.teleport(point.x, player.getY(), point.z, player.getYRot(), player.getXRot(), KEEP_ROTATION);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        if (trail) {
            Fx.dash(player);
        }
    }

    private static double horizontal(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
