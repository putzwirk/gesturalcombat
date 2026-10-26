package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.client.ClientAbilityDefaults;
import com.putzwirk.gesturalcombat.client.ClientSetup;
import com.putzwirk.gesturalcombat.client.ClientTickHandler;
import com.putzwirk.gesturalcombat.client.anim.CameraEffects;
import com.putzwirk.gesturalcombat.client.dev.UiProbe;
import com.putzwirk.gesturalcombat.client.fx.ParticleSetup;
import com.putzwirk.gesturalcombat.client.fx.TrailEvents;
import com.putzwirk.gesturalcombat.client.hud.GestureHud;
import com.putzwirk.gesturalcombat.client.render.FractureBlockRenderer;
import com.putzwirk.gesturalcombat.client.render.HitboxDebugEvents;
import com.putzwirk.gesturalcombat.client.render.ThrownChakramRenderer;
import com.putzwirk.gesturalcombat.client.render.ThrownShieldRenderer;
import com.putzwirk.gesturalcombat.entity.ModEntities;
import com.putzwirk.gesturalcombat.particle.FxType;
import com.putzwirk.gesturalcombat.particle.ModParticles;
import com.putzwirk.gesturalcombat.world.ModTerrain;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public final class GesturalcombatFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricNetwork.registerClient();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientTickHandler.tick();
            CameraEffects.tick();
            UiProbe.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientAbilityDefaults.onLoggingIn());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientTickHandler.logout());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
            TrailEvents.onRenderLevel(context.matrixStack(), context.camera(), partialTick);
            HitboxDebugEvents.onRenderLevel(context.matrixStack(), context.camera());
        });
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft minecraft = Minecraft.getInstance();
            GestureHud.render(graphics, tickCounter.getGameTimeDeltaPartialTick(false),
                    minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        });
        EntityRendererRegistry.register(ModEntities.THROWN_SHIELD.get(), ThrownShieldRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_CHAKRAM.get(), ThrownChakramRenderer::new);
        BlockEntityRendererRegistry.register(ModTerrain.FRACTURE_ENTITY.get(), FractureBlockRenderer::new);
        for (FxType type : FxType.values()) {
            ParticleFactoryRegistry.getInstance().register(ModParticles.type(type),
                    sprites -> ParticleSetup.provider(type, sprites));
        }
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                GesturalcombatFabric.wrap(ResourceLocation.fromNamespaceAndPath(Gesturalcombat.MODID, "icons"),
                        ClientSetup.iconReloadListener()));
    }
}
