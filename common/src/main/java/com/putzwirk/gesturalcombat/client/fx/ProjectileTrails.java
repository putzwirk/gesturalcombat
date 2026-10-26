package com.putzwirk.gesturalcombat.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.putzwirk.gesturalcombat.entity.ThrownChakramEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ProjectileTrails {
    private static final long LIFE_MS = 220L;
    private static final int MAX_SAMPLES = 16;
    private static final double WIDTH = 0.11;
    private static final int TINT = 0xFFFFFF;
    private static final Map<Integer, Deque<Sample>> TRAILS = new HashMap<>();

    private ProjectileTrails() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            TRAILS.clear();
            return;
        }
        long now = System.currentTimeMillis();
        List<Integer> seen = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownChakramEntity)) {
                continue;
            }
            int id = entity.getId();
            seen.add(id);
            Deque<Sample> samples = TRAILS.computeIfAbsent(id, key -> new ArrayDeque<>());
            samples.addLast(new Sample(entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0), now));
            while (samples.size() > MAX_SAMPLES) {
                samples.removeFirst();
            }
        }
        Iterator<Map.Entry<Integer, Deque<Sample>>> iterator = TRAILS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Deque<Sample>> entry = iterator.next();
            Deque<Sample> samples = entry.getValue();
            while (!samples.isEmpty() && now - samples.peekFirst().time() > LIFE_MS) {
                samples.removeFirst();
            }
            if (samples.size() < 2 || (!seen.contains(entry.getKey()) && now - samples.peekLast().time() > LIFE_MS)) {
                iterator.remove();
            }
        }
    }

    public static void clear() {
        TRAILS.clear();
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (TRAILS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TrailRenderTypes.TRAIL);
        PoseStack.Pose pose = poseStack.last();
        Vec3 origin = camera.getPosition();
        for (Deque<Sample> samples : TRAILS.values()) {
            draw(consumer, pose, origin, new ArrayList<>(samples), now);
        }
        buffers.endBatch(TrailRenderTypes.TRAIL);
    }

    private static void draw(VertexConsumer consumer, PoseStack.Pose pose, Vec3 origin, List<Sample> samples, long now) {
        int count = samples.size();
        if (count < 2) {
            return;
        }
        float r = ((TINT >> 16) & 0xFF) / 255.0f;
        float g = ((TINT >> 8) & 0xFF) / 255.0f;
        float b = (TINT & 0xFF) / 255.0f;
        for (int i = 1; i < count; i++) {
            Sample previous = samples.get(i - 1);
            Sample current = samples.get(i);
            Vec3 delta = current.position().subtract(previous.position());
            if (delta.lengthSqr() < 1.0e-6) {
                continue;
            }
            Vec3 side = delta.cross(new Vec3(0.0, 1.0, 0.0));
            if (side.lengthSqr() < 1.0e-6) {
                side = new Vec3(1.0, 0.0, 0.0);
            }
            side = side.normalize().scale(WIDTH);
            float fadePrevious = fade(now, previous.time());
            float fadeCurrent = fade(now, current.time());
            if (fadeCurrent <= 0.0f) {
                continue;
            }
            Vec3 a0 = previous.position().add(side);
            Vec3 a1 = previous.position().subtract(side);
            Vec3 b0 = current.position().add(side);
            Vec3 b1 = current.position().subtract(side);
            float v0 = 1.0f - fadePrevious;
            float v1 = 1.0f - fadeCurrent;
            vertex(consumer, pose, origin, a0, r, g, b, 0.02f * fadePrevious, 0.0f, v0);
            vertex(consumer, pose, origin, b0, r, g, b, 0.02f * fadeCurrent, 0.0f, v1);
            vertex(consumer, pose, origin, b1, r, g, b, 0.34f * fadeCurrent, 1.0f, v1);
            vertex(consumer, pose, origin, a1, r, g, b, 0.34f * fadePrevious, 1.0f, v0);
        }
    }

    private static float fade(long now, long time) {
        long age = now - time;
        if (age >= LIFE_MS) {
            return 0.0f;
        }
        return 1.0f - age / (float) LIFE_MS;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 origin, Vec3 at,
                               float r, float g, float b, float a, float u, float v) {
        Vec3 local = at.subtract(origin);
        consumer.addVertex(pose.pose(), (float) local.x, (float) local.y, (float) local.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0.0f, 1.0f, 0.0f)
                ;
    }

    private record Sample(Vec3 position, long time) {
    }
}
