package com.putzwirk.gesturalcombat.client.fx;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT)
public final class TrailEvents {
    private TrailEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        TrailRenderer.render(event.getPoseStack(), event.getCamera(), event.getPartialTick());
        ProjectileTrails.render(event.getPoseStack(), event.getCamera());
    }
}
