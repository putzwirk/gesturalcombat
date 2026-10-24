package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.OutLockPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OutLock {
    private static final Map<UUID, Boolean> LOCKED = new HashMap<>();

    private OutLock() {
    }

    public static void lock(ServerPlayer player, boolean offhand) {
        LOCKED.put(player.getUUID(), offhand);
        ModNetwork.sendToPlayer(player, new OutLockPacket(offhand, true));
    }

    public static void unlock(ServerPlayer player) {
        Boolean offhand = LOCKED.remove(player.getUUID());
        if (offhand != null) {
            ModNetwork.sendToPlayer(player, new OutLockPacket(offhand, false));
        }
    }

    public static boolean isLocked(ServerPlayer player, boolean offhand) {
        Boolean hand = LOCKED.get(player.getUUID());
        return hand != null && hand == offhand;
    }

    public static boolean any(ServerPlayer player) {
        return LOCKED.containsKey(player.getUUID());
    }

    public static void clear() {
        LOCKED.clear();
    }
}
