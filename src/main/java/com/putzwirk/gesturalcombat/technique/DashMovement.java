package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DashMovement {
    private static final double STEP = 0.55;
    private static final double MIN_STEP = 0.12;

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
        return run(player, level, destination, maxTicks, holdTicks, STEP);
    }

    public static int run(ServerPlayer player, Level level, Vec3 destination, int maxTicks, int holdTicks, double step) {
        Vec3 from = player.position();
        Vec3 end = clamp(player, level, from, destination);
        int hold = Math.max(1, holdTicks);
        double distance = horizontal(from, end);
        if (distance < MIN_STEP) {
            for (int i = 1; i <= hold; i++) {
                GestureScheduler.schedule(level, i, () -> brake(player, level));
            }
            return 0;
        }
        int ticks = Math.max(1, Math.min(maxTicks, (int) Math.ceil(distance / Math.max(MIN_STEP, step))));
        double total = 0.0;
        double[] weights = new double[ticks];
        for (int i = 0; i < ticks; i++) {
            double u = (i + 0.5) / ticks;
            weights[i] = 0.40 + 0.60 * Math.sin(Math.PI * u);
            total += weights[i];
        }
        Vec3 delta = new Vec3(end.x - from.x, 0.0, end.z - from.z);
        for (int i = 1; i <= ticks; i++) {
            Vec3 perTick = delta.scale(weights[i - 1] / total);
            GestureScheduler.schedule(level, i, () -> push(player, level, perTick));
        }
        for (int i = 1; i <= hold; i++) {
            GestureScheduler.schedule(level, ticks + i, () -> brake(player, level));
        }
        return ticks;
    }

    private static void push(ServerPlayer player, Level level, Vec3 perTick) {
        if (!player.isAlive() || player.isRemoved() || player.level() != level) {
            return;
        }
        player.setDeltaMovement(perTick.x, player.getDeltaMovement().y, perTick.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0f;
    }

    private static void brake(ServerPlayer player, Level level) {
        if (!player.isAlive() || player.isRemoved() || player.level() != level) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(0.0, motion.y, 0.0);
        player.hurtMarked = true;
        player.fallDistance = 0.0f;
    }

    public static void hold(ServerPlayer player, Level level, int ticks) {
        for (int i = 1; i <= Math.max(1, ticks); i++) {
            GestureScheduler.schedule(level, i, () -> brake(player, level));
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

    private static double horizontal(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
