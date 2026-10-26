package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;

public final class AbilityEvents {
    private AbilityEvents() {
    }

    public static void onLogin(ServerPlayer player) {
        AbilitySync.send(player);
        com.putzwirk.gesturalcombat.server.DisabledPlayers.syncTo(player);
        OutLock.unlock(player);
    }

    public static void onRespawn(ServerPlayer player) {
        AbilitySync.send(player);
        com.putzwirk.gesturalcombat.server.DisabledPlayers.syncTo(player);
    }

    public static void onChangedDimension(ServerPlayer player) {
        AbilitySync.send(player);
        com.putzwirk.gesturalcombat.server.DisabledPlayers.syncTo(player);
    }

    public static AbilityReloadListener reloadListener() {
        return new AbilityReloadListener();
    }
}
