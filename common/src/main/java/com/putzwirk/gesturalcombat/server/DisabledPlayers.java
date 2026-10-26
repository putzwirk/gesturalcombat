package com.putzwirk.gesturalcombat.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.network.DisabledPacket;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.platform.Platform;
import net.minecraft.server.level.ServerPlayer;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DisabledPlayers {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> DISABLED = new LinkedHashMap<>();
    private static boolean loaded;

    private DisabledPlayers() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = file();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("players")) {
                return;
            }
            for (JsonElement element : root.getAsJsonObject().getAsJsonArray("players")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                try {
                    UUID id = UUID.fromString(entry.get("uuid").getAsString());
                    String name = entry.has("name") ? entry.get("name").getAsString() : id.toString();
                    DISABLED.put(id, name);
                } catch (RuntimeException ignored) {
                }
            }
        } catch (RuntimeException | java.io.IOException e) {
            Gesturalcombat.LOGGER.warn("gesturalcombat could not read {}", file, e);
        }
    }

    public static boolean isDisabled(ServerPlayer player) {
        return player != null && isDisabled(player.getUUID());
    }

    public static boolean isDisabled(UUID id) {
        load();
        return DISABLED.containsKey(id);
    }

    public static List<String> names() {
        load();
        return new ArrayList<>(DISABLED.values());
    }

    public static boolean disable(UUID id, String name) {
        load();
        boolean added = DISABLED.put(id, name == null || name.isBlank() ? id.toString() : name) == null;
        if (added) {
            save();
            GestureCastService.forget(id);
        }
        sync();
        return added;
    }

    public static boolean disable(ServerPlayer player) {
        return disable(player.getUUID(), player.getGameProfile().getName());
    }

    public static boolean enable(UUID id) {
        load();
        boolean removed = DISABLED.remove(id) != null;
        if (removed) {
            save();
            GestureCastService.forget(id);
        }
        sync();
        return removed;
    }

    public static boolean enable(ServerPlayer player) {
        return enable(player.getUUID());
    }

    public static void sync() {
        DisabledPacket packet = packet();
        for (ServerPlayer online : players()) {
            ModNetwork.sendToPlayer(online, packet);
        }
    }

    public static void syncTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, packet());
    }

    private static DisabledPacket packet() {
        load();
        List<String> ids = new ArrayList<>();
        for (UUID id : new LinkedHashSet<>(DISABLED.keySet())) {
            ids.add(id.toString());
        }
        return new DisabledPacket(ids);
    }

    private static List<ServerPlayer> players() {
        List<ServerPlayer> players = new ArrayList<>();
        var server = com.putzwirk.gesturalcombat.platform.ServerRef.get();
        if (server == null) {
            return players;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            players.add(player);
        }
        return players;
    }

    private static void save() {
        Path file = file();
        if (file == null) {
            return;
        }
        JsonArray array = new JsonArray();
        for (Map.Entry<UUID, String> entry : DISABLED.entrySet()) {
            JsonObject object = new JsonObject();
            object.addProperty("uuid", entry.getKey().toString());
            object.addProperty("name", entry.getValue());
            array.add(object);
        }
        JsonObject root = new JsonObject();
        root.add("players", array);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (RuntimeException | java.io.IOException e) {
            Gesturalcombat.LOGGER.warn("gesturalcombat could not write {}", file, e);
        }
    }

    private static Path file() {
        try {
            return Platform.get().configDirectory().resolve(Gesturalcombat.MODID + "-disabled.json");
        } catch (RuntimeException e) {
            return null;
        }
    }
}
