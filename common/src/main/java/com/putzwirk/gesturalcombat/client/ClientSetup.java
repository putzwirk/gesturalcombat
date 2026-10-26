package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.client.hud.AbilityIcons;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static SimplePreparableReloadListener<Void> iconReloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void value, ResourceManager manager, ProfilerFiller profiler) {
                AbilityIcons.invalidate();
            }
        };
    }
}
