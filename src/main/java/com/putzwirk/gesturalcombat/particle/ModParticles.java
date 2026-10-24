package com.putzwirk.gesturalcombat.particle;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Gesturalcombat.MODID);

    private static final Map<FxType, RegistryObject<SimpleParticleType>> ENTRIES = new EnumMap<>(FxType.class);

    static {
        for (FxType type : FxType.values()) {
            ENTRIES.put(type, TYPES.register(type.id(), () -> new SimpleParticleType(false)));
        }
    }

    private ModParticles() {
    }

    public static RegistryObject<SimpleParticleType> holder(FxType type) {
        return ENTRIES.get(type);
    }

    public static SimpleParticleType type(FxType type) {
        return ENTRIES.get(type).get();
    }

    public static FxType byOrdinal(int ordinal) {
        FxType[] values = FxType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FxType.BURST;
    }
}
