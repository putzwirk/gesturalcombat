package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CastContext {
    private static final Map<UUID, Integer> SWIPES = new HashMap<>();

    private CastContext() {
    }

    public static void swipe(ServerPlayer player, int swipe) {
        if (SWIPES.size() > 512) {
            SWIPES.clear();
        }
        SWIPES.put(player.getUUID(), swipe);
    }

    public static int swipe(ServerPlayer player) {
        return SWIPES.getOrDefault(player.getUUID(), 1);
    }
}
