package com.putzwirk.gesturalcombat.technique.hitbox;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public record Hitbox(Shape shape, double range, double angle, double width, double height,
                     double verticalOffset, double forwardOffset, int maxTargets, boolean lineOfSight) {
    public enum Shape {
        ARC,
        VERTICAL_ARC,
        BOX,
        SPHERE
    }

    private static final int ARC_SAMPLES = 11;
    private static final int PITCH_SAMPLES = 13;
    private static final double VERTICAL_GRACE = 0.25;

    public static Hitbox arc(double range, double angle) {
        return new Hitbox(Shape.ARC, range, angle, 1.0, 3.6, 0.0, 0.0, 16, false);
    }

    public static Hitbox verticalArc(double range, double angle, double height) {
        return new Hitbox(Shape.VERTICAL_ARC, range, angle, 1.0, height, 0.0, 0.0, 16, false);
    }

    public static Hitbox box(double length, double width, double height) {
        return new Hitbox(Shape.BOX, length, 0.0, width, height, 0.0, 0.0, 16, true);
    }

    public static Hitbox sphere(double radius) {
        return new Hitbox(Shape.SPHERE, radius, 360.0, 1.0, 2.2, 0.0, 0.0, 16, false);
    }

    public Hitbox withRange(double value) {
        return new Hitbox(shape, Math.max(0.1, value), angle, width, height, verticalOffset, forwardOffset, maxTargets, lineOfSight);
    }

    public Hitbox withAngle(double value) {
        return new Hitbox(shape, range, Math.max(1.0, value), width, height, verticalOffset, forwardOffset, maxTargets, lineOfSight);
    }

    public Hitbox withMaxTargets(int value) {
        return new Hitbox(shape, range, angle, width, height, verticalOffset, forwardOffset, Math.max(1, value), lineOfSight);
    }

    public Hitbox withLineOfSight(boolean value) {
        return new Hitbox(shape, range, angle, width, height, verticalOffset, forwardOffset, maxTargets, value);
    }

    public List<LivingEntity> query(ServerPlayer player) {
        return query(player, player.level());
    }

    public List<LivingEntity> query(ServerPlayer player, Level level) {
        double reach = Math.max(0.1, range);
        AABB search = player.getBoundingBox().inflate(reach + 3.0);
        List<LivingEntity> hits = new ArrayList<>();
        List<Double> distances = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (contains(player, entity) && (!lineOfSight || visible(player, level, entity))) {
                hits.add(entity);
                distances.add(distance(player, entity));
            }
        }
        Integer[] order = new Integer[hits.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble(distances::get));
        List<LivingEntity> out = new ArrayList<>();
        for (int i = 0; i < order.length && out.size() < maxTargets; i++) {
            out.add(hits.get(order[i]));
        }
        return out;
    }

    public boolean contains(ServerPlayer player, LivingEntity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3 facing = facing(player.getYRot());
        return switch (shape) {
            case ARC -> inArc(entity, eye, facing);
            case VERTICAL_ARC -> inVerticalArc(entity, eye, facing, player.getXRot());
            case BOX -> inBox(player, entity, eye, facing);
            case SPHERE -> inSphere(player, entity);
        };
    }

    public static Vec3 facing(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    private boolean inArc(LivingEntity entity, Vec3 eye, Vec3 facing) {
        AABB box = entity.getBoundingBox();
        double low = eye.y + verticalOffset - height * 0.5 - VERTICAL_GRACE;
        double high = eye.y + verticalOffset + height * 0.5 + VERTICAL_GRACE;
        if (box.maxY < low || box.minY > high) {
            return false;
        }
        double maxDistance = range + entity.getBbWidth() * 0.5;
        double halfAngle = Math.toDegrees(Math.min(360.0, Math.max(1.0, angle)) * 0.5);
        for (Vec3 point : ringPoints(entity)) {
            Vec3 delta = point.subtract(eye);
            Vec3 flat = new Vec3(delta.x, 0.0, delta.z);
            double distance = flat.length();
            if (distance > maxDistance) {
                continue;
            }
            if (distance < 0.35) {
                return true;
            }
            double offset = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, flat.normalize().dot(facing)))));
            if (offset <= halfAngle) {
                return true;
            }
        }
        return false;
    }

    private boolean inVerticalArc(LivingEntity entity, Vec3 eye, Vec3 facing, double aimPitch) {
        AABB box = entity.getBoundingBox();
        Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
        double lateral = width * 0.5;
        double halfSize = Math.max(box.getXsize(), box.getZsize()) * 0.5;
        AABB probe = box.inflate(lateral);
        Vec3 center = box.getCenter().subtract(eye);
        if (Math.abs(center.dot(right)) - halfSize > lateral) {
            return false;
        }
        double heightSpan = Math.toDegrees(Math.atan2(height * 0.5, Math.max(0.5, range)));
        double halfSpan = Math.toRadians(Math.min(180.0, Math.max(angle * 0.5, heightSpan)));
        Vec3 start = eye.add(0.0, verticalOffset, 0.0).add(facing.scale(forwardOffset));
        double aim = Math.toRadians(aimPitch);
        for (int i = 0; i < PITCH_SAMPLES; i++) {
            double t = PITCH_SAMPLES == 1 ? 0.5 : i / (double) (PITCH_SAMPLES - 1);
            double pitch = aim - halfSpan + 2.0 * halfSpan * t;
            Vec3 dir = new Vec3(facing.x * Math.cos(pitch), -Math.sin(pitch), facing.z * Math.cos(pitch));
            Vec3 end = start.add(dir.scale(range));
            if (probe.contains(start) || probe.clip(start, end).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean inBox(ServerPlayer player, LivingEntity entity, Vec3 eye, Vec3 facing) {
        Vec3 start = eye.add(facing.scale(forwardOffset)).add(0.0, verticalOffset, 0.0);
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = entity.getBoundingBox().inflate(width * 0.5);
        return box.clip(start, end).isPresent() || box.contains(start);
    }

    private boolean inSphere(ServerPlayer player, LivingEntity entity) {
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5 + verticalOffset, 0.0);
        double distance = Math.sqrt(entity.getBoundingBox().distanceToSqr(center));
        return distance <= range;
    }

    public void outline(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 facing = facing(player.getYRot());
        int samples = 48;
        switch (shape) {
            case ARC -> {
                double half = Math.toRadians(Math.min(360.0, Math.max(1.0, angle)) * 0.5);
                for (int i = 0; i <= samples; i++) {
                    double theta = -half + 2.0 * half * (i / (double) samples);
                    point(level, eye.add(facing.yRot((float) theta).scale(range)).add(0.0, verticalOffset, 0.0));
                }
            }
            case VERTICAL_ARC -> {
                double heightSpan = Math.toDegrees(Math.atan2(height * 0.5, Math.max(0.5, range)));
                double half = Math.toRadians(Math.min(180.0, Math.max(angle * 0.5, heightSpan)));
                double aim = Math.toRadians(player.getXRot());
                Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
                for (int i = 0; i <= samples; i++) {
                    double pitch = aim - half + 2.0 * half * (i / (double) samples);
                    Vec3 dir = new Vec3(facing.x * Math.cos(pitch), -Math.sin(pitch), facing.z * Math.cos(pitch));
                    point(level, eye.add(dir.scale(range)).add(0.0, verticalOffset, 0.0));
                    Vec3 at = eye.add(dir.scale(range)).add(0.0, verticalOffset, 0.0);
                    point(level, at.add(right.scale(width * 0.5)));
                    point(level, at.add(right.scale(-width * 0.5)));
                }
            }
            case BOX -> {
                for (int i = 0; i <= 12; i++) {
                    double along = range * (i / 12.0);
                    Vec3 at = eye.add(player.getLookAngle().scale(along)).add(0.0, verticalOffset, 0.0);
                    Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
                    point(level, at.add(right.scale(width * 0.5)));
                    point(level, at.add(right.scale(-width * 0.5)));
                }
            }
            case SPHERE -> {
                for (int i = 0; i < samples; i++) {
                    double theta = Math.PI * 2.0 * (i / (double) samples);
                    point(level, eye.add(Math.cos(theta) * range, verticalOffset, Math.sin(theta) * range));
                }
            }
        }
    }

    private static void point(ServerLevel level, Vec3 at) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static boolean visible(ServerPlayer player, Level level, LivingEntity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3[] points = {
                entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0),
                entity.position().add(0.0, entity.getBbHeight() * 0.9, 0.0)
        };
        for (Vec3 point : points) {
            BlockHitResult wall = level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (wall.getType() == HitResult.Type.MISS || wall.getLocation().distanceToSqr(eye) >= point.distanceToSqr(eye) - 1.0E-4) {
                return true;
            }
        }
        return false;
    }

    private static List<Vec3> ringPoints(LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        double y = box.getCenter().y;
        List<Vec3> points = new ArrayList<>(ARC_SAMPLES * 2 + 2);
        for (int i = 0; i < ARC_SAMPLES; i++) {
            double t = i / (double) (ARC_SAMPLES - 1);
            double x = box.minX + (box.maxX - box.minX) * t;
            points.add(new Vec3(x, y, box.minZ));
            points.add(new Vec3(x, y, box.maxZ));
        }
        points.add(new Vec3(box.minX, y, (box.minZ + box.maxZ) * 0.5));
        points.add(new Vec3(box.maxX, y, (box.minZ + box.maxZ) * 0.5));
        return points;
    }

    private static double distance(ServerPlayer player, LivingEntity entity) {
        return Math.sqrt(entity.getBoundingBox().distanceToSqr(player.getEyePosition())) - entity.getBbWidth() * 0.5;
    }

    public static Hitbox parse(String shapeName, double range, double angle, double width, double height,
                               double verticalOffset, double forwardOffset, int maxTargets, boolean lineOfSight) {
        Shape shape;
        try {
            shape = Shape.valueOf(shapeName == null ? "ARC" : shapeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            shape = Shape.ARC;
        }
        return new Hitbox(shape, Math.max(0.1, range), Math.max(1.0, angle), Math.max(0.1, width),
                Math.max(0.1, height), verticalOffset, forwardOffset, Math.max(1, maxTargets), lineOfSight);
    }
}
