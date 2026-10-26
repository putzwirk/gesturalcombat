package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.client.ClientSetup;
import com.putzwirk.gesturalcombat.client.fx.ParticleSetup;
import com.putzwirk.gesturalcombat.client.hud.GestureHud;
import com.putzwirk.gesturalcombat.client.render.FractureBlockRenderer;
import com.putzwirk.gesturalcombat.client.render.ThrownChakramRenderer;
import com.putzwirk.gesturalcombat.client.render.ThrownShieldRenderer;
import com.putzwirk.gesturalcombat.entity.ModEntities;
import com.putzwirk.gesturalcombat.particle.FxType;
import com.putzwirk.gesturalcombat.particle.ModParticles;
import com.putzwirk.gesturalcombat.world.ModTerrain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeClientMod {
    private ForgeClientMod() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_SHIELD.get(), ThrownShieldRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_CHAKRAM.get(), ThrownChakramRenderer::new);
        event.registerBlockEntityRenderer(ModTerrain.FRACTURE_ENTITY.get(), FractureBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        for (FxType type : FxType.values()) {
            event.registerSpriteSet(ModParticles.type(type), sprites -> ParticleSetup.provider(type, sprites));
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ClientSetup.iconReloadListener());
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("gesture_hud",
                (gui, graphics, partialTick, width, height) -> GestureHud.render(graphics, partialTick, width, height));
    }
}
