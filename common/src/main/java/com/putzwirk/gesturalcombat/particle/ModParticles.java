package com.putzwirk.gesturalcombat.particle;

import com.putzwirk.gesturalcombat.platform.Platform;
import com.putzwirk.gesturalcombat.platform.Reg;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.EnumMap;
import java.util.Map;

public final class ModParticles {
    private static final Map<FxType, Reg<SimpleParticleType>> ENTRIES = new EnumMap<>(FxType.class);

    private ModParticles() {
    }

    public static void registerAll() {
        for (FxType type : FxType.values()) {
            ENTRIES.put(type, Platform.get().registerParticle(type.id(), () -> new SimpleParticleType(false) {
            }));
        }
    }

    public static Reg<SimpleParticleType> holder(FxType type) {
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
