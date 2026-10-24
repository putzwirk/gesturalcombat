package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.network.AbilitySyncPacket;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class AbilitySync {
    private AbilitySync() {
    }

    public static void broadcast() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        AbilitySyncPacket packet = packet();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(player, packet);
        }
    }

    public static void send(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, packet());
    }

    private static AbilitySyncPacket packet() {
        return new AbilitySyncPacket(AbilityConfigs.abilitiesJson(), AbilityConfigs.weaponsJson());
    }
}
