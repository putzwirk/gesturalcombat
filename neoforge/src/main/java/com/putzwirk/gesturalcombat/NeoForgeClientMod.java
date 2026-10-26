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
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeClientMod {
    private NeoForgeClientMod() {
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
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Gesturalcombat.MODID, "gesture_hud"),
                (graphics, deltaTracker) -> GestureHud.render(graphics,
                        deltaTracker.getGameTimeDeltaPartialTick(false),
                        net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                        net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight()));
    }
}
