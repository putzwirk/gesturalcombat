package com.putzwirk.gesturalcombat.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class TrailRenderer {
    private TrailRenderer() {
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        if (TrailManager.trails().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (com.putzwirk.gesturalcombat.Config.INSTANCE.debugCasts.get()) {
            int samples = 0;
            for (TrailManager.Trail trail : TrailManager.trails()) {
                samples += trail.samples.size();
            }
            com.putzwirk.gesturalcombat.Gesturalcombat.LOGGER.info("gesturalcombat trails: {} ribbons, {} samples",
                    TrailManager.trails().size(), samples);
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TrailRenderTypes.TRAIL);
        PoseStack.Pose pose = poseStack.last();
        Vec3 origin = camera.getPosition();
        for (TrailManager.Trail trail : TrailManager.trails()) {
            draw(consumer, pose, origin, trail, now);
        }
        buffers.endBatch(TrailRenderTypes.TRAIL);
    }

    private static void draw(VertexConsumer consumer, PoseStack.Pose pose, Vec3 origin,
                             TrailManager.Trail trail, long now) {
        List<TrailManager.Sample> samples = new ArrayList<>(trail.samples);
        int count = samples.size();
        if (count < 2) {
            return;
        }
        float r = ((trail.tint >> 16) & 0xFF) / 255.0f;
        float g = ((trail.tint >> 8) & 0xFF) / 255.0f;
        float b = (trail.tint & 0xFF) / 255.0f;
        for (int i = 1; i < count; i++) {
            TrailManager.Sample previous = samples.get(i - 1);
            TrailManager.Sample current = samples.get(i);
            float fadeCurrent = fade(now, current.time(), trail.fadeMillis);
            if (fadeCurrent <= 0.0f) {
                continue;
            }
            float fadePrevious = fade(now, previous.time(), trail.fadeMillis);
            float v0 = 1.0f - fadePrevious;
            float v1 = 1.0f - fadeCurrent;
            Vec3 tipPrevious = previous.tip();
            Vec3 tipCurrent = current.tip();
            Vec3 basePrevious = previous.base();
            Vec3 baseCurrent = current.base();
            Vec3 reachPrevious = tipPrevious.subtract(basePrevious).scale(0.22);
            Vec3 reachCurrent = tipCurrent.subtract(baseCurrent).scale(0.22);
            if (tipCurrent.distanceToSqr(tipPrevious) < 1.0e-5) {
                continue;
            }
            quad(consumer, pose, origin,
                    basePrevious, tipPrevious.add(reachPrevious),
                    baseCurrent, tipCurrent.add(reachCurrent),
                    r, g, b, 0.04f * fadePrevious, 0.34f * fadePrevious,
                    0.04f * fadeCurrent, 0.34f * fadeCurrent, v0, v1);
            Vec3 corePrevious = previous.base().lerp(previous.tip(), 0.42);
            Vec3 coreCurrent = current.base().lerp(current.tip(), 0.42);
            quad(consumer, pose, origin,
                    corePrevious, tipPrevious.add(reachPrevious),
                    coreCurrent, tipCurrent.add(reachCurrent),
                    r, g, b, 0.12f * fadePrevious, 0.82f * fadePrevious,
                    0.12f * fadeCurrent, 0.82f * fadeCurrent, v0, v1);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 origin,
                             Vec3 base0, Vec3 tip0, Vec3 base1, Vec3 tip1,
                             float r, float g, float b,
                             float baseAlpha0, float tipAlpha0,
                             float baseAlpha1, float tipAlpha1,
                             float v0, float v1) {
        vertex(consumer, pose, origin, base0, r, g, b, baseAlpha0, 0.0f, v0);
        vertex(consumer, pose, origin, tip0, r, g, b, tipAlpha0, 1.0f, v0);
        vertex(consumer, pose, origin, tip1, r, g, b, tipAlpha1, 1.0f, v1);
        vertex(consumer, pose, origin, base1, r, g, b, baseAlpha1, 0.0f, v1);
    }

    private static float fade(long now, long time, long fadeMillis) {
        long age = now - time;
        if (age >= fadeMillis) {
            return 0.0f;
        }
        return 1.0f - age / (float) fadeMillis;
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
}
