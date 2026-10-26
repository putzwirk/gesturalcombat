package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MomentumState {
    public static final int CAP = 5;
    private static final int DECAY_TICKS = 60;
    private static final Map<UUID, Integer> STACKS = new HashMap<>();
    private static final Map<UUID, Long> LAST_GAIN = new HashMap<>();

    private MomentumState() {
    }

    public static int get(ServerPlayer player) {
        return STACKS.getOrDefault(player.getUUID(), 0);
    }

    public static void add(ServerPlayer player, int amount) {
        int value = Math.min(CAP, get(player) + Math.max(0, amount));
        STACKS.put(player.getUUID(), value);
        LAST_GAIN.put(player.getUUID(), player.level().getGameTime());
    }

    public static int consume(ServerPlayer player) {
        int value = get(player);
        STACKS.remove(player.getUUID());
        LAST_GAIN.remove(player.getUUID());
        return value;
    }

    public static void clear() {
        STACKS.clear();
        LAST_GAIN.clear();
    }

        public static void tick(MinecraftServer server) {
        if (STACKS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> iterator = STACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                LAST_GAIN.remove(entry.getKey());
                continue;
            }
            long last = LAST_GAIN.getOrDefault(entry.getKey(), player.level().getGameTime());
            long now = player.level().getGameTime();
            if (now - last < DECAY_TICKS) {
                continue;
            }
            if (entry.getValue() <= 1) {
                iterator.remove();
                LAST_GAIN.remove(entry.getKey());
            } else {
                entry.setValue(entry.getValue() - 1);
                LAST_GAIN.put(entry.getKey(), now);
            }
        }
    }
}
