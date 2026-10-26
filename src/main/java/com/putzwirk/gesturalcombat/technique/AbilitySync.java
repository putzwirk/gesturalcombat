package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.network.AbilitySyncPacket;
import com.putzwirk.gesturalcombat.network.ConfigPayload;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class AbilitySync {
    private AbilitySync() {
    }

    private static boolean forceAudit;

    public static void broadcastAudit() {
        forceAudit = true;
        try {
            broadcast();
        } finally {
            forceAudit = false;
        }
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
        AbilityConfigs.ensureLoaded();
        if (!AbilityConfigs.loaded()) {
            Gesturalcombat.LOGGER.warn("Ability config sync has no data: {} techniques, {} profiles",
                    AbilityConfigs.count(), com.putzwirk.gesturalcombat.weapon.WeaponProfiles.count());
        }
        String audit = forceAudit || com.putzwirk.gesturalcombat.Config.INSTANCE.debugWeapons.get()
                ? com.putzwirk.gesturalcombat.weapon.WeaponAudit.summary() : "";
        return new AbilitySyncPacket(
                ConfigPayload.pack(AbilityConfigs.abilitiesJson()),
                ConfigPayload.pack(AbilityConfigs.weaponsJson()),
                ConfigPayload.pack(audit));
    }
}
