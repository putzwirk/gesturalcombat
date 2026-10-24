package com.putzwirk.gesturalcombat.weapon;

import com.putzwirk.gesturalcombat.core.GestureShape;
import net.minecraft.world.item.ItemStack;

public final class WeaponRules {
    private WeaponRules() {
    }

    public static boolean blocked(ItemStack stack, GestureShape shape) {
        String family = WeaponProfiles.family(stack);
        if (family == null || shape == null) {
            return false;
        }
        if ("chakram".equalsIgnoreCase(family) && shape == GestureShape.LINE_V_DOWN) {
            return WeaponProfiles.special(stack, shape) == null;
        }
        return false;
    }
}
