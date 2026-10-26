package com.putzwirk.gesturalcombat.client.render;

import com.putzwirk.gesturalcombat.Gesturalcombat;

public final class HitboxDebugEvents {
    private HitboxDebugEvents() {
    }

        public static void onRenderLevel(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     net.minecraft.client.Camera camera) {
        HitboxDebug.render(poseStack, camera);
    }
}
