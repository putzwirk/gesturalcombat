package com.putzwirk.gesturalcombat.weapon;

import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.Technique;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class WeaponRules {
    private static final Set<String> HEAVY = Set.of("claymore", "glaive", "halberd", "greataxe", "greathammer", "heavy_axe");
    private static final Set<GestureShape> HEAVY_LOCKED = java.util.EnumSet.of(GestureShape.CIRCLE,
            GestureShape.CARET, GestureShape.V, GestureShape.LINE_V_UP);
    private static final int HEAVY_TAIL = 12;

    private WeaponRules() {
    }

    public static boolean heavy(ItemStack stack) {
        String family = WeaponProfiles.family(stack);
        return family != null && HEAVY.contains(family.toLowerCase(java.util.Locale.ROOT));
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
