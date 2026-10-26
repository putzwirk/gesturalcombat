package com.putzwirk.gesturalcombat.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.putzwirk.gesturalcombat.client.ClientState;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class HitboxDebug {
    private static final int LIFETIME = 24;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private HitboxDebug() {
    }

    private record Entry(Hitbox box, Vec3 eye, float yaw, float pitch, int born, int color) {
    }

    public static void capture(Technique technique, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = mc.player.getItemInHand(hand);
        Hitbox box = AbilityConfigs.hitbox(technique, stack, null);
        if (box == null) {
            return;
        }
        double factor = 1.0;
        double absolute = com.putzwirk.gesturalcombat.technique.EffectSupport.weaponReach(stack);
        box = box.withRange(absolute > 0.0
                ? absolute * factor
                : box.range() * factor * com.putzwirk.gesturalcombat.weapon.WeaponProfiles.reachMultiplier(stack));
        int color = technique.archetype() == null ? 0xFFFFFFFF : technique.archetype().color();
        ENTRIES.add(new Entry(box, mc.player.getEyePosition(), mc.player.getYRot(), mc.player.getXRot(),
                ClientState.ticks, color));
        while (ENTRIES.size() > 12) {
            ENTRIES.remove(0);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mc.options.getCameraType().isFirstPerson() || !mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }
        Iterator<Entry> iterator = ENTRIES.iterator();
        while (iterator.hasNext()) {
            if (ClientState.ticks - iterator.next().born() > LIFETIME) {
                iterator.remove();
            }
        }
        if (ENTRIES.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        Vec3 origin = camera.getPosition();
        for (Entry entry : ENTRIES) {
            draw(consumer, pose, entry, origin);
        }
        buffers.endBatch(RenderType.lines());
    }

    private static void draw(VertexConsumer consumer, PoseStack.Pose pose, Entry entry, Vec3 origin) {
        Hitbox box = entry.box();
        Vec3 eye = entry.eye();
        Vec3 facing = Hitbox.facing(entry.yaw());
        float r = ((entry.color() >> 16) & 0xFF) / 255.0f;
        float g = ((entry.color() >> 8) & 0xFF) / 255.0f;
        float b = (entry.color() & 0xFF) / 255.0f;
        float a = 0.85f;
        switch (box.shape()) {
            case ARC -> {
                double low = eye.y + box.verticalOffset() - box.height() * 0.5;
                double high = eye.y + box.verticalOffset() + box.height() * 0.5;
                double half = Math.toRadians(Math.min(360.0, Math.max(1.0, box.angle())) * 0.5);
                int steps = 24;
                Vec3 prevLow = null;
                Vec3 prevHigh = null;
                for (int i = 0; i <= steps; i++) {
                    double theta = -half + 2.0 * half * (i / (double) steps);
                    Vec3 dir = facing.yRot((float) theta);
                    Vec3 lowPoint = new Vec3(eye.x + dir.x * box.range(), low, eye.z + dir.z * box.range());
                    Vec3 highPoint = new Vec3(eye.x + dir.x * box.range(), high, eye.z + dir.z * box.range());
                    if (prevLow != null) {
                        line(consumer, pose, origin, prevLow, lowPoint, r, g, b, a);
                        line(consumer, pose, origin, prevHigh, highPoint, r, g, b, a);
                    }
                    if (i % 6 == 0) {
                        line(consumer, pose, origin, lowPoint, highPoint, r, g, b, a * 0.6f);
                    }
                    if (i == 0 || i == steps) {
                        Vec3 mid = new Vec3(lowPoint.x, (low + high) * 0.5, lowPoint.z);
                        line(consumer, pose, origin, eye, mid, r, g, b, a * 0.5f);
                    }
                    prevLow = lowPoint;
                    prevHigh = highPoint;
                }
            }
            case VERTICAL_ARC -> {
                double heightSpan = Math.toDegrees(Math.atan2(box.height() * 0.5, Math.max(0.5, box.range())));
                double half = Math.toRadians(Math.min(180.0, Math.max(box.angle() * 0.5, heightSpan)));
                double aim = Math.toRadians(entry.pitch());
                int steps = 24;
                Vec3 prev = null;
                for (int i = 0; i <= steps; i++) {
                    double pitch = aim - half + 2.0 * half * (i / (double) steps);
                    Vec3 point = eye.add(ray(facing, pitch).scale(box.range()));
                    if (prev != null) {
                        line(consumer, pose, origin, prev, point, r, g, b, a);
                    }
                    if (i == 0 || i == steps) {
                        line(consumer, pose, origin, eye, point, r, g, b, a * 0.5f);
                    }
                    prev = point;
                }
            }
            case BOX -> {
                Vec3 look = ray(facing, Math.toRadians(entry.pitch()));
                Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
                Vec3 up = right.cross(look).normalize();
                Vec3 start = eye.add(facing.scale(box.forwardOffset()));
                Vec3 end = start.add(look.scale(box.range()));
                double hw = box.width() * 0.5;
                double hh = box.height() * 0.5;
                for (int side = -1; side <= 1; side += 2) {
                    for (int vertical = -1; vertical <= 1; vertical += 2) {
                        Vec3 offset = right.scale(hw * side).add(up.scale(hh * vertical));
                        line(consumer, pose, origin, start.add(offset), end.add(offset), r, g, b, a);
                    }
                }
                ring(consumer, pose, origin, start, right, up, hw, hh, r, g, b, a);
                ring(consumer, pose, origin, end, right, up, hw, hh, r, g, b, a);
            }
            case SPHERE -> {
                Vec3 center = new Vec3(eye.x, eye.y + box.verticalOffset(), eye.z);
                int steps = 32;
                for (int plane = 0; plane < 3; plane++) {
                    Vec3 prev = null;
                    for (int i = 0; i <= steps; i++) {
                        double angle = Math.PI * 2.0 * i / steps;
                        Vec3 point = center.add(circlePoint(plane, angle, box.range()));
                        if (prev != null) {
                            line(consumer, pose, origin, prev, point, r, g, b, a);
                        }
                        prev = point;
                    }
                }
            }
        }
    }

    private static void ring(VertexConsumer consumer, PoseStack.Pose pose, Vec3 origin, Vec3 center,
                             Vec3 right, Vec3 up, double hw, double hh, float r, float g, float b, float a) {
        Vec3 p1 = center.add(right.scale(hw)).add(up.scale(hh));
        Vec3 p2 = center.add(right.scale(-hw)).add(up.scale(hh));
        Vec3 p3 = center.add(right.scale(-hw)).add(up.scale(-hh));
        Vec3 p4 = center.add(right.scale(hw)).add(up.scale(-hh));
        line(consumer, pose, origin, p1, p2, r, g, b, a);
        line(consumer, pose, origin, p2, p3, r, g, b, a);
        line(consumer, pose, origin, p3, p4, r, g, b, a);
        line(consumer, pose, origin, p4, p1, r, g, b, a);
    }

    private static Vec3 ray(Vec3 facing, double pitch) {
        return new Vec3(facing.x * Math.cos(pitch), -Math.sin(pitch), facing.z * Math.cos(pitch));
    }

    private static Vec3 circlePoint(int plane, double angle, double radius) {
        double c = Math.cos(angle) * radius;
        double s = Math.sin(angle) * radius;
        return switch (plane) {
            case 0 -> new Vec3(c, 0.0, s);
            case 1 -> new Vec3(c, s, 0.0);
            default -> new Vec3(0.0, c, s);
        };
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose, Vec3 origin, Vec3 from, Vec3 to,
                             float r, float g, float b, float a) {
        Vec3 a1 = from.subtract(origin);
        Vec3 b1 = to.subtract(origin);
        vertex(consumer, pose, a1, r, g, b, a);
        vertex(consumer, pose, b1, r, g, b, a);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at,
                               float r, float g, float b, float a) {
        consumer.vertex(pose.pose(), (float) at.x, (float) at.y, (float) at.z)
                .color(r, g, b, a)
                .normal(pose.normal(), 0.0f, 1.0f, 0.0f)
                .endVertex();
    }

    @Nullable
    public static Hitbox peek() {
        return ENTRIES.isEmpty() ? null : ENTRIES.get(ENTRIES.size() - 1).box();
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
