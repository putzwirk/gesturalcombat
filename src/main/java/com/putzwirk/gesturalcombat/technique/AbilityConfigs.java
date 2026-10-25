package com.putzwirk.gesturalcombat.technique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.weapon.WeaponProfile;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AbilityConfigs {
    private static final Map<String, AbilityConfig> BY_ID = new HashMap<>();
    private static final Map<String, List<String>> CATEGORY_ANIMATIONS = new HashMap<>();
    private static JsonObject abilitiesJson = new JsonObject();
    private static JsonObject weaponsJson = new JsonObject();

    private AbilityConfigs() {
    }

    public static void clear() {
        BY_ID.clear();
        CATEGORY_ANIMATIONS.clear();
        abilitiesJson = new JsonObject();
        weaponsJson = new JsonObject();
    }

    public static boolean hasTechniques() {
        return count() > 0;
    }

    public static boolean hasWeapons() {
        return com.putzwirk.gesturalcombat.weapon.WeaponProfiles.count() > 0;
    }

    public static boolean loaded() {
        return hasTechniques() && hasWeapons();
    }

    public static void ensureLoaded() {
        if (loaded()) {
            return;
        }
        JsonObject bundledAbilities = readBundled("/data/gesturalcombat/abilities.json");
        JsonObject bundledWeapons = readBundled("/data/gesturalcombat/weapons.json");
        if (bundledAbilities == null || bundledWeapons == null) {
            return;
        }
        load(bundledAbilities, bundledWeapons);
    }

    private static JsonObject readBundled(String path) {
        try (java.io.InputStream stream = AbilityConfigs.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (java.io.Reader reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (RuntimeException | java.io.IOException e) {
            return null;
        }
    }

    public static void load(JsonObject abilities, JsonObject weapons) {
        boolean hasAbilities = abilities != null && abilities.has("techniques");
        boolean hasWeapons = weapons != null && weapons.has("weapons");
        if (!hasAbilities && !hasWeapons) {
            return;
        }
        if (hasAbilities) {
            BY_ID.clear();
            abilitiesJson = abilities;
            for (Map.Entry<String, com.google.gson.JsonElement> entry : abilities.getAsJsonObject("techniques").entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                BY_ID.computeIfAbsent(entry.getKey(), AbilityConfig::new).read(entry.getValue().getAsJsonObject());
            }
        }
        if (hasWeapons) {
            CATEGORY_ANIMATIONS.clear();
            weaponsJson = weapons;
            readCategories();
            com.putzwirk.gesturalcombat.weapon.WeaponProfiles.load(weapons);
        }
        Techniques.applyConfig();
    }

    private static void readCategories() {
        if (!weaponsJson.has("categories") || !weaponsJson.get("categories").isJsonObject()) {
            return;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> entry : weaponsJson.getAsJsonObject("categories").entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            List<String> animations = new ArrayList<>();
            for (com.google.gson.JsonElement element : entry.getValue().getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    animations.add(element.getAsString());
                }
            }
            if (!animations.isEmpty()) {
                CATEGORY_ANIMATIONS.put(entry.getKey().toLowerCase(Locale.ROOT), animations);
            }
        }
    }

    public static List<String> categoryAnimations(@Nullable String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }
        return CATEGORY_ANIMATIONS.getOrDefault(category.toLowerCase(Locale.ROOT), List.of());
    }

    public static void loadFromJson(String abilities, String weapons) {
        if ((abilities == null || abilities.isBlank()) && (weapons == null || weapons.isBlank())) {
            return;
        }
        JsonObject ab = parse(abilities);
        JsonObject wp = parse(weapons);
        load(ab, wp);
    }

    private static JsonObject parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    public static String abilitiesJson() {
        return abilitiesJson.toString();
    }

    public static String weaponsJson() {
        return weaponsJson.toString();
    }

    @Nullable
    public static AbilityConfig of(String id) {
        return BY_ID.get(id);
    }

    @Nullable
    public static AbilityConfig of(Technique technique) {
        return technique == null ? null : BY_ID.get(technique.id());
    }

    @Nullable
    public static AbilityConfig effective(Technique technique, ItemStack stack) {
        AbilityConfig base = of(technique);
        WeaponProfile profile = WeaponProfiles.profile(stack);
        AbilityConfig override = profile == null ? null : profile.technique(technique.id());
        if (base == null || override == null) {
            return override != null ? override : base;
        }
        AbilityConfig merged = new AbilityConfig(base.id());
        merged.merge(base);
        merged.merge(override);
        return merged;
    }

    public static Hitbox hitbox(Technique technique, ItemStack stack, Hitbox fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.hitbox() == null ? fallback : config.hitbox();
    }

    public static float damageMultiplier(Technique technique, ItemStack stack, double fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.damageMultiplier() == null ? (float) fallback : config.damageMultiplier().floatValue();
    }

    public static double knockback(Technique technique, ItemStack stack, double fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.knockback() == null ? fallback : config.knockback();
    }

    public static double lift(Technique technique, ItemStack stack, double fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.lift() == null ? fallback : config.lift();
    }

    public static double selfKnockback(Technique technique, ItemStack stack, double fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.selfKnockback() == null ? fallback : config.selfKnockback();
    }

    public static int cooldownTicks(Technique technique, ItemStack stack) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.cooldownTicks() == null ? technique.cooldownTicks() : config.cooldownTicks();
    }

    public static int hits(Technique technique) {
        AbilityConfig config = of(technique);
        return config == null || config.hits() == null ? technique.effect().hitCount() : Math.max(1, config.hits());
    }

    public static int hitInterval(Technique technique) {
        AbilityConfig config = of(technique);
        return config == null || config.hitInterval() == null ? technique.effect().hitIntervalTicks() : Math.max(0, config.hitInterval());
    }

    public static int windupTicks(Technique technique) {
        AbilityConfig config = of(technique);
        return config == null || config.windupTicks() == null ? technique.effect().windupTicks() : Math.max(0, config.windupTicks());
    }

    public static int count() {
        return BY_ID.size();
    }

    public static int hits(Technique technique, ItemStack stack, int fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.hits() == null ? fallback : Math.max(1, config.hits());
    }

    public static int hitInterval(Technique technique, ItemStack stack, int fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.hitInterval() == null ? fallback : Math.max(0, config.hitInterval());
    }

    public static int hitIntervalEnd(Technique technique, ItemStack stack, int fallback) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.hitIntervalEnd() == null ? fallback : Math.max(1, config.hitIntervalEnd());
    }

    public static int hitIntervalEnd(Technique technique) {
        AbilityConfig config = of(technique);
        return config == null || config.hitIntervalEnd() == null ? hitInterval(technique) : Math.max(1, config.hitIntervalEnd());
    }

    public static int[] hitDelays(Technique technique, ItemStack stack) {
        return hitDelays(technique, stack,
                Math.max(1, technique.effect().hitCount()),
                Math.max(0, technique.effect().hitIntervalTicks()),
                Math.max(0, technique.effect().hitIntervalEndTicks()));
    }

    public static int[] hitDelays(Technique technique, ItemStack stack, int fallbackHits, int fallbackStart, int fallbackEnd) {
        int count = Math.max(1, hits(technique, stack, fallbackHits));
        int start = hitInterval(technique, stack, fallbackStart);
        int end = hitIntervalEnd(technique, stack, fallbackEnd);
        int[] delays = new int[count];
        int at = 0;
        for (int i = 1; i < count; i++) {
            double t = count <= 2 ? 0.0 : (i - 1) / (double) (count - 2);
            at += Math.max(1, (int) Math.round(start + (end - start) * t));
            delays[i] = at;
        }
        return delays;
    }

    public static int windupTicks(Technique technique, ItemStack stack) {
        AbilityConfig config = effective(technique, stack);
        return config == null || config.windupTicks() == null ? technique.effect().windupTicks() : Math.max(0, config.windupTicks());
    }

    public static JsonObject writeAbilities() {
        JsonObject root = new JsonObject();
        JsonObject techniques = new JsonObject();
        for (Map.Entry<String, AbilityConfig> entry : BY_ID.entrySet()) {
            techniques.add(entry.getKey(), entry.getValue().write());
        }
        root.add("techniques", techniques);
        return root;
    }
}
