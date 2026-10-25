package com.putzwirk.gesturalcombat.client.fx;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.particle.FxType;
import com.putzwirk.gesturalcombat.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ParticleSetup {
    private ParticleSetup() {
    }

    @SubscribeEvent
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        for (FxType type : FxType.values()) {
            if (type == FxType.WAVE) {
                event.registerSpriteSet(ModParticles.holder(type).get(), GroundWaveParticle.Provider::new);
            } else {
                event.registerSpriteSet(ModParticles.holder(type).get(), SlashParticle.Provider::new);
            }
        }
    }
}
