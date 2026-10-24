package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.OutLockPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OutLock {
    private static final long MAX_MS = 8000L;
    private static final Map<UUID, Lock> LOCKED = new HashMap<>();

    private OutLock() {
    }

    public static void lock(ServerPlayer player, boolean offhand) {
        LOCKED.put(player.getUUID(), new Lock(offhand, System.currentTimeMillis() + MAX_MS));
        ModNetwork.sendToPlayer(player, new OutLockPacket(offhand, true));
    }

    public static void unlock(ServerPlayer player) {
        Lock lock = LOCKED.remove(player.getUUID());
        if (lock != null) {
            ModNetwork.sendToPlayer(player, new OutLockPacket(lock.offhand, false));
        }
    }

    public static boolean isLocked(ServerPlayer player, boolean offhand) {
        Lock lock = LOCKED.get(player.getUUID());
        if (lock == null) {
            return false;
        }
        if (System.currentTimeMillis() > lock.expiresAt) {
            LOCKED.remove(player.getUUID());
            ModNetwork.sendToPlayer(player, new OutLockPacket(lock.offhand, false));
            return false;
        }
        return lock.offhand == offhand;
    }

    public static boolean any(ServerPlayer player) {
        return isLocked(player, true) || isLocked(player, false);
    }

    private record Lock(boolean offhand, long expiresAt) {
    }

    public static void clear() {
        LOCKED.clear();
    }
}
