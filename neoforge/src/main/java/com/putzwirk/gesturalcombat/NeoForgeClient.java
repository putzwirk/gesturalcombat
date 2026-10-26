package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.client.ClientAbilityDefaults;
import com.putzwirk.gesturalcombat.client.ClientTickHandler;
import com.putzwirk.gesturalcombat.client.anim.CameraEffects;
import com.putzwirk.gesturalcombat.client.dev.UiProbe;
import com.putzwirk.gesturalcombat.client.fx.TrailEvents;
import com.putzwirk.gesturalcombat.client.render.HitboxDebugEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT)
public final class NeoForgeClient {
    private NeoForgeClient() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        ClientTickHandler.tick();
        CameraEffects.tick();
        UiProbe.tick();
    }

    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientAbilityDefaults.onLoggingIn();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTickHandler.logout();
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float[] angles = CameraEffects.angles();
        if (angles == null) {
            return;
        }
        event.setYaw(event.getYaw() + angles[0]);
        event.setPitch(event.getPitch() + angles[1]);
        event.setRoll(event.getRoll() + angles[2]);
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        if (event.getPlayer() != net.minecraft.client.Minecraft.getInstance().player) {
            return;
        }
        event.setNewFovModifier(CameraEffects.fov(event.getNewFovModifier()));
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        TrailEvents.onRenderLevel(event.getPoseStack(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        HitboxDebugEvents.onRenderLevel(event.getPoseStack(), event.getCamera());
    }
}
