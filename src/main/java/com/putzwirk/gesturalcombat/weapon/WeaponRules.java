package com.putzwirk.gesturalcombat.weapon;

import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.Technique;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

public final class WeaponRules {
    public record Tuning(double damage, double cooldown, int stun, double reach) {
    }

    private static final Set<String> HEAVY = Set.of("claymore", "glaive", "halberd", "greataxe", "greathammer",
            "heavy_axe", "mace");
    private static final Set<String> LIGHT = Set.of("rapier", "sickle", "soul_knife", "katana");
    private static final Set<String> AREA = Set.of("battlestaff", "twinblade", "warglaive", "scythe");
    private static final Set<GestureShape> HEAVY_LOCKED = java.util.EnumSet.of(GestureShape.CIRCLE,
            GestureShape.CARET, GestureShape.V);
    private static final int HEAVY_TAIL = 12;

    private static final Tuning DEFAULT = new Tuning(0.66, 1.25, 4, 1.0);

    private static final Map<String, Tuning> TUNING = Map.ofEntries(
            Map.entry("soul_knife", new Tuning(1.90, 1.20, 2, 0.95)),
            Map.entry("sickle", new Tuning(1.18, 1.15, 2, 0.95)),
            Map.entry("rapier", new Tuning(1.20, 1.15, 2, 0.97)),
            Map.entry("katana", new Tuning(1.04, 1.15, 2, 0.97)),
            Map.entry("sword", new Tuning(0.79, 1.15, 3, 1.0)),
            Map.entry("chakram", new Tuning(1.18, 1.20, 3, 1.0)),
            Map.entry("trident", new Tuning(1.32, 1.35, 3, 1.0)),
            Map.entry("battlestaff", new Tuning(0.99, 1.25, 2, 1.0)),
            Map.entry("warglaive", new Tuning(1.27, 1.30, 2, 1.0)),
            Map.entry("scythe", new Tuning(0.87, 1.25, 4, 1.0)),
            Map.entry("claymore", new Tuning(1.10, 1.35, 8, 1.0)),
            Map.entry("glaive", new Tuning(1.19, 1.30, 8, 1.0)),
            Map.entry("halberd", new Tuning(1.13, 1.30, 8, 1.0)),
            Map.entry("greataxe", new Tuning(1.06, 1.30, 9, 1.0)),
            Map.entry("greathammer", new Tuning(0.89, 1.40, 9, 1.0)),
            Map.entry("heavy_axe", new Tuning(1.04, 1.35, 8, 1.0)),
            Map.entry("mace", new Tuning(0.90, 1.35, 8, 1.0)),
            Map.entry("axe", new Tuning(0.72, 1.25, 3, 1.0)),
            Map.entry("spear", new Tuning(1.06, 1.25, 3, 1.0))
    );

    private WeaponRules() {
    }

    public static Tuning tuning(ItemStack stack) {
        String family = WeaponProfiles.family(stack);
        if (family == null) {
            com.putzwirk.gesturalcombat.core.Archetype archetype =
                    com.putzwirk.gesturalcombat.core.Archetype.of(stack);
            if (archetype != null) {
                return TUNING.getOrDefault(archetype.id(), DEFAULT);
            }
            return DEFAULT;
        }
        return TUNING.getOrDefault(family.toLowerCase(java.util.Locale.ROOT), DEFAULT);
    }

    public static boolean light(ItemStack stack) {
        String family = WeaponProfiles.family(stack);
        return family != null && LIGHT.contains(family.toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean area(ItemStack stack) {
        String family = WeaponProfiles.family(stack);
        return family != null && AREA.contains(family.toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean heavy(ItemStack stack) {
        String family = WeaponProfiles.family(stack);
        return family != null && HEAVY.contains(family.toLowerCase(java.util.Locale.ROOT));
    }

    public static int hitStunTicks(ItemStack stack) {
        return tuning(stack).stun();
    }

    public static double damageFactor(ItemStack stack) {
        return tuning(stack).damage();
    }

    public static double cooldownFactor(ItemStack stack) {
        return tuning(stack).cooldown();
    }

    public static double reachFactor(ItemStack stack) {
        return tuning(stack).reach();
    }

    public static int busyTicks(Technique technique, ItemStack stack) {
        if (technique == null || !heavy(stack)) {
            return 0;
        }
        int hits = Math.max(1, AbilityConfigs.hits(technique));
        int interval = Math.max(0, AbilityConfigs.hitInterval(technique));
        return Math.max(8, AbilityConfigs.windupTicks(technique) + (hits - 1) * interval + HEAVY_TAIL);
    }

    public static boolean blocked(ItemStack stack, GestureShape shape) {
        String family = WeaponProfiles.family(stack);
        if (family == null || shape == null) {
            return false;
        }
        if ("chakram".equalsIgnoreCase(family) && shape == GestureShape.LINE_V_DOWN) {
            return true;
        }
        return HEAVY.contains(family.toLowerCase(java.util.Locale.ROOT)) && HEAVY_LOCKED.contains(shape);
    }
}
