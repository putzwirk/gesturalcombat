package com.putzwirk.gesturalcombat.client.fx;

import com.putzwirk.gesturalcombat.Gesturalcombat;

public final class TrailEvents {
    private TrailEvents() {
    }

        public static void onRenderLevel(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     net.minecraft.client.Camera camera, float partialTick) {
        TrailRenderer.render(poseStack, camera, partialTick);
        ProjectileTrails.render(poseStack, camera);
    }
}
