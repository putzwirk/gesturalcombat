package com.putzwirk.gesturalcombat.technique;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AbilityConfig {
    private final String id;
    @Nullable
    private Archetype archetype;
    private final List<GestureShape> gestures = new ArrayList<>();
    @Nullable
    private Hitbox hitbox;
    @Nullable
    private Double damageMultiplier;
    @Nullable
    private Double knockback;
    @Nullable
    private Double lift;
    @Nullable
    private Double selfKnockback;
    @Nullable
    private Integer cooldownTicks;
    @Nullable
    private Integer hits;
    @Nullable
    private Integer hitInterval;
    @Nullable
    private Integer windupTicks;

    public AbilityConfig(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Nullable
    public Archetype archetype() {
        return archetype;
    }

    public List<GestureShape> gestures() {
        return gestures;
    }

    @Nullable
    public Hitbox hitbox() {
        return hitbox;
    }

    @Nullable
    public Double damageMultiplier() {
        return damageMultiplier;
    }

    @Nullable
    public Double knockback() {
        return knockback;
    }

    @Nullable
    public Double lift() {
        return lift;
    }

    @Nullable
    public Double selfKnockback() {
        return selfKnockback;
    }

    @Nullable
    public Integer cooldownTicks() {
        return cooldownTicks;
    }

    @Nullable
    public Integer hits() {
        return hits;
    }

    @Nullable
    public Integer hitInterval() {
        return hitInterval;
    }

    @Nullable
    public Integer windupTicks() {
        return windupTicks;
    }

    public void read(JsonObject json) {
        if (json.has("archetype")) {
            archetype = parseEnum(Archetype.class, json.get("archetype").getAsString());
        }
        if (json.has("gesture")) {
            gestures.clear();
            readGestures(json.get("gesture"), gestures);
        }
        if (json.has("hitbox") && json.get("hitbox").isJsonObject()) {
            hitbox = readHitbox(json.getAsJsonObject("hitbox"), hitbox);
        }
        damageMultiplier = readDouble(json, "damage_multiplier", damageMultiplier);
        knockback = readDouble(json, "knockback", knockback);
        lift = readDouble(json, "lift", lift);
        selfKnockback = readDouble(json, "self_knockback", selfKnockback);
        cooldownTicks = readInt(json, "cooldown_ticks", cooldownTicks);
        hits = readInt(json, "hits", hits);
        hitInterval = readInt(json, "hit_interval", hitInterval);
        windupTicks = readInt(json, "windup_ticks", windupTicks);
    }

    public void merge(AbilityConfig override) {
        if (override.archetype != null) {
            archetype = override.archetype;
        }
        if (!override.gestures.isEmpty()) {
            gestures.clear();
            gestures.addAll(override.gestures);
        }
        if (override.hitbox != null) {
            hitbox = override.hitbox;
        }
        if (override.damageMultiplier != null) {
            damageMultiplier = override.damageMultiplier;
        }
        if (override.knockback != null) {
            knockback = override.knockback;
        }
        if (override.lift != null) {
            lift = override.lift;
        }
        if (override.selfKnockback != null) {
            selfKnockback = override.selfKnockback;
        }
        if (override.cooldownTicks != null) {
            cooldownTicks = override.cooldownTicks;
        }
        if (override.hits != null) {
            hits = override.hits;
        }
        if (override.hitInterval != null) {
            hitInterval = override.hitInterval;
        }
        if (override.windupTicks != null) {
            windupTicks = override.windupTicks;
        }
    }

    public JsonObject write() {
        JsonObject json = new JsonObject();
        if (archetype != null) {
            json.addProperty("archetype", archetype.name().toLowerCase(Locale.ROOT));
        }
        if (!gestures.isEmpty()) {
            JsonArray array = new JsonArray();
            for (GestureShape shape : gestures) {
                array.add(shape.name());
            }
            json.add("gesture", array);
        }
        if (hitbox != null) {
            JsonObject box = new JsonObject();
            box.addProperty("shape", hitbox.shape().name());
            box.addProperty("range", hitbox.range());
            box.addProperty("angle", hitbox.angle());
            box.addProperty("width", hitbox.width());
            box.addProperty("height", hitbox.height());
            box.addProperty("vertical_offset", hitbox.verticalOffset());
            box.addProperty("forward_offset", hitbox.forwardOffset());
            box.addProperty("max_targets", hitbox.maxTargets());
            box.addProperty("line_of_sight", hitbox.lineOfSight());
            json.add("hitbox", box);
        }
        put(json, "damage_multiplier", damageMultiplier);
        put(json, "knockback", knockback);
        put(json, "lift", lift);
        put(json, "self_knockback", selfKnockback);
        put(json, "cooldown_ticks", cooldownTicks);
        put(json, "hits", hits);
        put(json, "hit_interval", hitInterval);
        put(json, "windup_ticks", windupTicks);
        return json;
    }

    private static void put(JsonObject json, String key, Number value) {
        if (value != null) {
            json.addProperty(key, value);
        }
    }

    private static void readGestures(JsonElement element, List<GestureShape> out) {
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                addGesture(entry.getAsString(), out);
            }
        } else {
            addGesture(element.getAsString(), out);
        }
    }

    private static void addGesture(String name, List<GestureShape> out) {
        GestureShape shape = parseEnum(GestureShape.class, name);
        if (shape != null && shape.isKnown() && !out.contains(shape)) {
            out.add(shape);
        }
    }

    private static Hitbox readHitbox(JsonObject json, @Nullable Hitbox base) {
        Hitbox fallback = base == null ? Hitbox.arc(2.5, 120.0) : base;
        return Hitbox.parse(
                json.has("shape") ? json.get("shape").getAsString() : fallback.shape().name(),
                readDouble(json, "range", fallback.range()),
                readDouble(json, "angle", fallback.angle()),
                readDouble(json, "width", fallback.width()),
                readDouble(json, "height", fallback.height()),
                readDouble(json, "vertical_offset", fallback.verticalOffset()),
                readDouble(json, "forward_offset", fallback.forwardOffset()),
                readInt(json, "max_targets", fallback.maxTargets()),
                json.has("line_of_sight") ? json.get("line_of_sight").getAsBoolean() : fallback.lineOfSight());
    }

    @Nullable
    private static <T extends Enum<T>> T parseEnum(Class<T> type, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Double readDouble(JsonObject json, String key, @Nullable Double fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return json.get(key).getAsDouble();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static double readDouble(JsonObject json, String key, double fallback) {
        Double value = readDouble(json, key, (Double) fallback);
        return value == null ? fallback : value;
    }

    @Nullable
    private static Integer readInt(JsonObject json, String key, @Nullable Integer fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return json.get(key).getAsInt();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        Integer value = readInt(json, key, (Integer) fallback);
        return value == null ? fallback : value;
    }
}
