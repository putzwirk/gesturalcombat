package com.putzwirk.gesturalcombat.technique;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuardState {
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private GuardState() {
    }

    public static void begin(Player player, int ticks) {
        UNTIL.put(player.getUUID(), player.level().getGameTime() + Math.max(1, ticks));
    }

    public static void end(Player player) {
        UNTIL.remove(player.getUUID());
    }

    public static boolean isGuarding(Player player) {
        Long until = UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() > until) {
            UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }
}
