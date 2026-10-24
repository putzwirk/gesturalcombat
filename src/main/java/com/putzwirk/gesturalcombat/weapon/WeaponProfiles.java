package com.putzwirk.gesturalcombat.weapon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.putzwirk.gesturalcombat.core.GestureShape;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WeaponProfiles {
    private static final List<WeaponProfile> PROFILES = new ArrayList<>();
    private static final Map<Item, WeaponProfile> CACHE = new HashMap<>();
    private static boolean tagsPresent;
    private static final WeaponProfile NOT_FOUND = new WeaponProfile();

    private WeaponProfiles() {
    }

    public static void clear() {
        PROFILES.clear();
        CACHE.clear();
        tagsPresent = false;
    }

    public static void add(WeaponProfile profile) {
        PROFILES.add(profile);
        CACHE.clear();
        tagsPresent |= profile.hasTags();
    }

    public static void load(JsonElement root) {
        clear();
        if (root == null || !root.isJsonObject()) {
            return;
        }
        JsonObject json = root.getAsJsonObject();
        JsonElement list = json.has("weapons") ? json.get("weapons") : null;
        if (list == null || !list.isJsonArray()) {
            return;
        }
        for (JsonElement entry : (JsonArray) list) {
            if (!entry.isJsonObject()) {
                continue;
            }
            WeaponProfile profile = new WeaponProfile();
            profile.read(entry.getAsJsonObject());
            add(profile);
        }
    }

    public static void load(String rawJson) {
        try {
            load(JsonParser.parseString(rawJson));
        } catch (RuntimeException ignored) {
        }
    }

    @Nullable
    public static WeaponProfile profile(ItemStack stack) {
        if (stack.isEmpty() || PROFILES.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (CACHE.containsKey(item)) {
            WeaponProfile cached = CACHE.get(item);
            return cached == NOT_FOUND ? null : cached;
        }
        for (WeaponProfile profile : PROFILES) {
            if (!profile.matches(stack)) {
                continue;
            }
            if (!profile.hasTags()) {
                CACHE.put(item, profile);
            }
            return profile;
        }
        if (!tagsPresent) {
            CACHE.put(item, NOT_FOUND);
        }
        return null;
    }

    public static int count() {
        return PROFILES.size();
    }

    @Nullable
    public static com.putzwirk.gesturalcombat.core.Archetype archetype(ItemStack stack) {
        WeaponProfile profile = profile(stack);
        return profile == null ? null : profile.archetype();
    }

    @Nullable
    public static WeaponSpecial special(ItemStack stack, GestureShape shape) {
        WeaponProfile profile = profile(stack);
        return profile == null ? null : profile.special(shape);
    }

    @javax.annotation.Nullable
    public static String family(ItemStack stack) {
        WeaponProfile profile = profile(stack);
        return profile == null ? null : profile.family();
    }

    public static double damageMultiplier(ItemStack stack) {
        WeaponProfile profile = profile(stack);
        return profile == null ? 1.0 : profile.damageMultiplier();
    }

    public static double reachMultiplier(ItemStack stack) {
        WeaponProfile profile = profile(stack);
        return profile == null ? 1.0 : profile.reachMultiplier();
    }

    public static double cooldownMultiplier(ItemStack stack) {
        WeaponProfile profile = profile(stack);
        return profile == null ? 1.0 : profile.cooldownMultiplier();
    }

    public static double reach(ItemStack stack) {
        WeaponProfile profile = profile(stack);
        return profile == null ? 0.0 : profile.reach();
    }

    public static List<String> animations(ItemStack stack) {
        WeaponProfile profile = profile(stack);
        return profile == null ? java.util.Collections.emptyList() : profile.animations();
    }
}
