package com.putzwirk.gesturalcombat.client.fx;

import com.putzwirk.gesturalcombat.particle.FxType;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public final class ParticleSetup {
    private ParticleSetup() {
    }

    public static ParticleProvider<SimpleParticleType> provider(FxType type, SpriteSet sprites) {
        return type == FxType.WAVE ? new GroundWaveParticle.Provider(sprites) : new SlashParticle.Provider(sprites);
    }
}
