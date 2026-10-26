package com.putzwirk.gesturalcombat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.putzwirk.gesturalcombat.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Config INSTANCE = new Config();

    private final List<Value<?>> values = new ArrayList<>();

    public final Value<Boolean> gesturesEnabled = new Value<>("gestures", "enabled", true);
    public final Value<Double> damageMultiplier = new Value<>("gestures", "damageMultiplier", 1.0);
    public final Value<Double> cooldownMultiplier = new Value<>("gestures", "cooldownMultiplier", 1.0);
    public final Value<Boolean> debugHitboxes = new Value<>("gestures", "debugHitboxes", false);
    public final Value<Boolean> weaponTrails = new Value<>("gestures", "weaponTrails", true);
    public final Value<Boolean> debugCasts = new Value<>("gestures", "debugCasts", false);
    public final Value<Boolean> debugWeapons = new Value<>("gestures", "debugWeapons", false);

    private Config() {
    }

    public static void load() {
        INSTANCE.read();
    }

    private void read() {
        Path file = path();
        if (file == null) {
            return;
        }
        if (!Files.exists(file)) {
            write(file);
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return;
            }
            JsonObject object = root.getAsJsonObject();
            for (Value<?> value : values) {
                JsonObject group = object.has(value.group) && object.get(value.group).isJsonObject()
                        ? object.getAsJsonObject(value.group) : null;
                if (group == null || !group.has(value.key)) {
                    continue;
                }
                value.read(group.get(value.key));
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private void write(Path file) {
        JsonObject root = new JsonObject();
        for (Value<?> value : values) {
            JsonObject group = root.has(value.group) && root.get(value.group).isJsonObject()
                    ? root.getAsJsonObject(value.group) : new JsonObject();
            group.add(value.key, value.json());
            root.add(value.group, group);
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private Path path() {
        try {
            return Platform.get().configDirectory().resolve(Gesturalcombat.MODID + ".json");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public final class Value<T> implements Supplier<T> {
        private final String group;
        private final String key;
        private final T fallback;
        private volatile T current;

        private Value(String group, String key, T fallback) {
            this.group = group;
            this.key = key;
            this.fallback = fallback;
            this.current = fallback;
            values.add(this);
        }

        @Override
        public T get() {
            return current;
        }

        private void read(JsonElement element) {
            try {
                if (fallback instanceof Boolean) {
                    current = (T) Boolean.valueOf(element.getAsBoolean());
                } else if (fallback instanceof Double) {
                    current = (T) Double.valueOf(element.getAsDouble());
                } else if (fallback instanceof Integer) {
                    current = (T) Integer.valueOf(element.getAsInt());
                } else if (fallback instanceof String) {
                    current = (T) element.getAsString();
                }
            } catch (RuntimeException ignored) {
            }
        }

        private JsonElement json() {
            if (fallback instanceof Boolean bool) {
                return new com.google.gson.JsonPrimitive(bool);
            }
            if (fallback instanceof Number number) {
                return new com.google.gson.JsonPrimitive(number);
            }
            return new com.google.gson.JsonPrimitive(String.valueOf(fallback));
        }
    }
}
