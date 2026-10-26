package com.putzwirk.gesturalcombat.weapon;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.technique.States;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Set;

public final class DualWield {
    public static final double COOLDOWN_SCALE = 0.92;
    public static final int LINK_TICKS = 10;
    public static final double ECHO_DAMAGE = 0.30;
    public static final double ECHO_REACH = 0.85;
    public static final double ECHO_ARC = 1.0;
    private static final Set<String> TWO_HANDED_FAMILIES = Set.of("katana", "battlestaff", "scythe");

    private DualWield() {
    }

    public static boolean paired(LivingEntity entity) {
        return entity != null && paired(entity.getMainHandItem(), entity.getOffhandItem());
    }

    public static boolean paired(ItemStack main, ItemStack offhand) {
        if (main == null || offhand == null || main.isEmpty() || offhand.isEmpty()) {
            return false;
        }
        String mainFamily = WeaponProfiles.family(main);
        String offFamily = WeaponProfiles.family(offhand);
        if (mainFamily == null || offFamily == null || !mainFamily.equalsIgnoreCase(offFamily)) {
            return false;
        }
        if (WeaponRules.heavy(main) || WeaponRules.heavy(offhand)) {
            return false;
        }
        if (twoHanded(main) || twoHanded(offhand)) {
            return false;
        }
        Archetype archetype = Archetype.of(main);
        if (archetype == Archetype.SHIELD || archetype == Archetype.BOW) {
            return false;
        }
        return Archetype.of(offhand) == archetype;
    }

    private static boolean twoHanded(ItemStack stack) {
        if (com.putzwirk.gesturalcombat.compat.BetterCombatBridge.isTwoHanded(stack)) {
            return true;
        }
        String family = WeaponProfiles.family(stack);
        return family != null && TWO_HANDED_FAMILIES.contains(family.toLowerCase(Locale.ROOT));
    }

    public static boolean link(ServerPlayer player, boolean offhand, long now) {
        return States.pairLink(player, offhand, now, LINK_TICKS);
    }
}
