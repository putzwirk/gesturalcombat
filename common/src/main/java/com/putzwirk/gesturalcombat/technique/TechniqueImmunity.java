package com.putzwirk.gesturalcombat.technique;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TechniqueImmunity {
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private TechniqueImmunity() {
    }

    public static void grant(Player player, int ticks) {
        UNTIL.put(player.getUUID(), player.level().getGameTime() + Math.max(1, ticks));
    }

    public static boolean isImmune(Player player) {
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
