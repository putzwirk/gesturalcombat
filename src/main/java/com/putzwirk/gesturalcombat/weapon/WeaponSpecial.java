package com.putzwirk.gesturalcombat.weapon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WeaponSpecial {
    public record EffectSpec(String id, int duration, int amplifier) {
    }

    @Nullable
    private GestureShape gesture;
    private int cooldownTicks = 120;
    private double damageMultiplier = 1.0;
    private double knockback = 0.4;
    private double lift = 0.0;
    private double heal = 0.0;
    private Hitbox hitbox = Hitbox.sphere(3.2);
    private String sound = "";
    private String particles = "";
    private final List<EffectSpec> targetEffects = new ArrayList<>();
    private final List<EffectSpec> selfEffects = new ArrayList<>();

    @Nullable
    public static WeaponSpecial read(JsonObject json) {
        WeaponSpecial special = new WeaponSpecial();
        if (json.has("gesture")) {
            try {
                special.gesture = GestureShape.valueOf(json.get("gesture").getAsString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (special.gesture == null) {
            return null;
        }
        special.cooldownTicks = Math.max(20, readInt(json, "cooldown_ticks", 120));
        special.damageMultiplier = readDouble(json, "damage_multiplier", 1.0);
        special.knockback = readDouble(json, "knockback", 0.4);
        special.lift = readDouble(json, "lift", 0.0);
        special.heal = readDouble(json, "heal", 0.0);
        special.sound = json.has("sound") ? json.get("sound").getAsString() : "";
        special.particles = json.has("particles") ? json.get("particles").getAsString() : "";
        if (json.has("hitbox") && json.get("hitbox").isJsonObject()) {
            JsonObject box = json.getAsJsonObject("hitbox");
            special.hitbox = Hitbox.parse(
                    box.has("shape") ? box.get("shape").getAsString() : "SPHERE",
                    readDouble(box, "range", 3.2),
                    readDouble(box, "angle", 360.0),
                    readDouble(box, "width", 1.0),
                    readDouble(box, "height", 3.0),
                    readDouble(box, "vertical_offset", 0.0),
                    readDouble(box, "forward_offset", 0.0),
                    readInt(box, "max_targets", 10),
                    box.has("line_of_sight") && box.get("line_of_sight").getAsBoolean());
        }
        readEffects(json, "target_effects", special.targetEffects);
        readEffects(json, "self_effects", special.selfEffects);
        return special;
    }

    private static void readEffects(JsonObject json, String key, List<EffectSpec> out) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return;
        }
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("id")) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.get("id").getAsString());
            if (id == null) {
                continue;
            }
            out.add(new EffectSpec(id.toString(), readInt(entry, "duration", 100), readInt(entry, "amplifier", 0)));
        }
    }

    public GestureShape gesture() {
        return gesture;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double knockback() {
        return knockback;
    }

    public double lift() {
        return lift;
    }

    public double heal() {
        return heal;
    }

    public Hitbox hitbox() {
        return hitbox;
    }

    public String sound() {
        return sound;
    }

    public String particles() {
        return particles;
    }

    public List<EffectSpec> targetEffects() {
        return targetEffects;
    }

    public List<EffectSpec> selfEffects() {
        return selfEffects;
    }

    private static double readDouble(JsonObject json, String key, double fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return json.get(key).getAsDouble();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return json.get(key).getAsInt();
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
