package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface TechniqueEffect {
    void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique);

    default int hitCount() {
        return 1;
    }

    default int hitIntervalTicks() {
        return 0;
    }

    default int windupTicks() {
        return 0;
    }
}
