package com.putzwirk.gesturalcombat.weapon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.AbilityConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WeaponProfile {
    private final List<String> ids = new ArrayList<>();
    private final List<TagKey<Item>> tags = new ArrayList<>();
    private final List<String> patterns = new ArrayList<>();
    @Nullable
    private Archetype archetype;
    @Nullable
    private String family;
    private double damageMultiplier = 1.0;
    private double reachMultiplier = 1.0;
    private double cooldownMultiplier = 1.0;
    private double reach = 0.0;
    @Nullable
    private Boolean twoHanded;
    private final List<String> animations = new ArrayList<>();
    private final java.util.EnumSet<GestureShape> gestures = java.util.EnumSet.noneOf(GestureShape.class);
    private final Map<String, AbilityConfig> techniques = new HashMap<>();
    private final List<WeaponSpecial> specials = new ArrayList<>();

    public void read(JsonObject json) {
        JsonElement items = json.get("items");
        if (items != null && items.isJsonArray()) {
            for (JsonElement entry : items.getAsJsonArray()) {
                addSelector(entry.getAsString());
            }
        } else if (items != null) {
            addSelector(items.getAsString());
        }
        if (json.has("archetype")) {
            try {
                archetype = Archetype.valueOf(json.get("archetype").getAsString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("family") && json.get("family").isJsonPrimitive()) {
            family = json.get("family").getAsString().trim().toLowerCase(Locale.ROOT);
        }
        damageMultiplier = read(json, "damage_multiplier", damageMultiplier);
        reachMultiplier = read(json, "reach_multiplier", reachMultiplier);
        cooldownMultiplier = read(json, "cooldown_multiplier", cooldownMultiplier);
        reach = Math.max(0.0, read(json, "reach", reach));
        if (json.has("two_handed") && json.get("two_handed").isJsonPrimitive()) {
            twoHanded = json.get("two_handed").getAsBoolean();
        }
        if (json.has("animations") && json.get("animations").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("animations")) {
                if (element.isJsonPrimitive()) {
                    animations.add(element.getAsString());
                }
            }
        }
        if (json.has("gestures") && json.get("gestures").isJsonArray()) {
            gestures.clear();
            for (JsonElement element : json.getAsJsonArray("gestures")) {
                if (element.isJsonPrimitive()) {
                    try {
                        gestures.add(GestureShape.valueOf(element.getAsString().trim().toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        if (json.has("specials") && json.get("specials").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("specials")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                WeaponSpecial special = WeaponSpecial.read(element.getAsJsonObject());
                if (special != null) {
                    specials.add(special);
                }
            }
        }
        if (json.has("techniques") && json.get("techniques").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("techniques").entrySet()) {
                AbilityConfig config = techniques.computeIfAbsent(entry.getKey(), AbilityConfig::new);
                if (entry.getValue().isJsonObject()) {
                    config.read(entry.getValue().getAsJsonObject());
                }
            }
        }
    }

    @Nullable
    public String family() {
        return family;
    }

    public boolean allows(GestureShape shape) {
        return gestures.isEmpty() || gestures.contains(shape);
    }

    private void addSelector(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            return;
        }
        if (value.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring(1));
            if (id != null) {
                tags.add(TagKey.create(Registries.ITEM, id));
            }
            return;
        }
        if (value.contains("*")) {
            patterns.add(value.toLowerCase(Locale.ROOT));
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id != null) {
            ids.add(id.toString());
        }
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        if (ids.contains(id.toString()) || matchesPattern(id)) {
            return true;
        }
        if (tags.isEmpty()) {
            return false;
        }
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTags() {
        return !tags.isEmpty();
    }

    private boolean matchesPattern(ResourceLocation id) {
        if (patterns.isEmpty()) {
            return false;
        }
        String value = id.toString().toLowerCase(Locale.ROOT);
        for (String pattern : patterns) {
            if (glob(pattern, value) || glob("minecraft:" + pattern, value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean glob(String pattern, String value) {
        int star = pattern.indexOf('*');
        if (star < 0) {
            return pattern.equals(value);
        }
        String prefix = pattern.substring(0, star);
        String suffix = pattern.substring(star + 1);
        return value.length() >= prefix.length() + suffix.length()
                && value.startsWith(prefix)
                && value.endsWith(suffix);
    }

    @Nullable
    public Archetype archetype() {
        return archetype;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double reachMultiplier() {
        return reachMultiplier;
    }

    public double cooldownMultiplier() {
        return cooldownMultiplier;
    }

    public double reach() {
        return reach;
    }

    @Nullable
    public Boolean twoHanded() {
        return twoHanded;
    }

    public List<String> animations() {
        return animations;
    }

    @Nullable
    public WeaponSpecial special(GestureShape shape) {
        for (WeaponSpecial special : specials) {
            if (special.gesture() == shape) {
                return special;
            }
        }
        return null;
    }

    @Nullable
    public AbilityConfig technique(String id) {
        return techniques.get(id);
    }

    private static double read(JsonObject json, String key, double fallback) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return json.get(key).getAsDouble();
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
