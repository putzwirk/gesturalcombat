package com.putzwirk.gesturalcombat.client.anim;

import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnimationCatalog {
    private AnimationCatalog() {
    }

    public static boolean twoHanded(ItemStack stack) {
        return BetterCombatBridge.isTwoHanded(stack);
    }

    public static int swingLength(Archetype archetype, ItemStack stack) {
        int base = switch (archetype) {
            case SWORD -> 10;
            case AXE -> 12;
            case SPEAR -> 9;
            case SHIELD -> 8;
            case BOW -> 14;
        };
        return twoHanded(stack) ? base + 2 : base;
    }

    public static int swingLength(int intervalTicks) {
        return intervalTicks <= 0 ? 0 : Math.max(6, intervalTicks * 2);
    }

    public static int fixedLength(GestureShape shape) {
        return switch (shape) {
            case V -> 20;
            case CARET -> 14;
            default -> 0;
        };
    }

    @Nullable
    public static String[] sequenceFor(Archetype archetype, GestureShape shape, ItemStack stack, int swipe, int swings) {
        if (archetype == null || shape == null || shape == GestureShape.UNKNOWN) {
            return null;
        }
        String family = WeaponProfiles.family(stack);
        if (throwsWeapon(family, shape)) {
            boolean requireThrow = "chakram".equalsIgnoreCase(family);
            String thrown = throwAnimation(stack, requireThrow);
            return thrown == null ? null : new String[]{thrown};
        }
        String[] pair = weaponSequence(shape, stack, swipe);
        if (pair == null) {
            pair = pairFor(archetype, shape, twoHanded(stack), swipe);
        }
        if (pair == null || pair.length == 0) {
            return null;
        }
        int count = Math.max(1, swings);
        String[] out = new String[count];
        for (int i = 0; i < count; i++) {
            out[i] = pair[i % pair.length];
        }
        return out;
    }

    private static boolean throwsWeapon(@Nullable String family, GestureShape shape) {
        if (family == null) {
            return false;
        }
        String value = family.toLowerCase(Locale.ROOT);
        if (value.equals("chakram")) {
            return shape == GestureShape.LINE_H || shape == GestureShape.CIRCLE || shape == GestureShape.ZIGZAG;
        }
        if (value.equals("trident") || value.equals("mace")) {
            return shape == GestureShape.ZIGZAG;
        }
        return false;
    }

    @Nullable
    private static String throwAnimation(ItemStack stack, boolean requireThrow) {
        String first = null;
        for (String id : WeaponProfiles.animations(stack)) {
            if (id == null || id.isBlank() || BetterCombatBridge.animation(id) == null) {
                continue;
            }
            if (id.toLowerCase(Locale.ROOT).contains("throw")) {
                return id;
            }
            if (first == null) {
                first = id;
            }
        }
        return requireThrow ? null : first;
    }

    @Nullable
    private static String[] weaponSequence(GestureShape shape, ItemStack stack, int swipe) {
        if (stack == null || stack.isEmpty() || shape == GestureShape.LINE_V_UP) {
            return null;
        }
        Kind[] order = preference(shape);
        if (order == null) {
            return null;
        }
        List<String> pool = pool(stack);
        if (pool.isEmpty()) {
            return null;
        }
        boolean twoHanded = twoHanded(stack);
        for (Kind kind : order) {
            List<String> picked = ofKind(pool, kind);
            if (picked.isEmpty()) {
                continue;
            }
            picked = preferredHand(picked, twoHanded);
            if (kind == Kind.HORIZONTAL) {
                picked = direction(picked, swipe);
            }
            return picked.toArray(new String[0]);
        }
        return preferredHand(pool, twoHanded).toArray(new String[0]);
    }

    @Nullable
    private static Kind[] preference(GestureShape shape) {
        return switch (shape) {
            case CIRCLE -> new Kind[]{Kind.SPIN};
            case LINE_H -> new Kind[]{Kind.HORIZONTAL, Kind.STAB};
            case ZIGZAG -> new Kind[]{Kind.HORIZONTAL, Kind.SPIN};
            case LINE_V_UP -> new Kind[]{Kind.RISING, Kind.STAB, Kind.VERTICAL};
            case LINE_V_DOWN -> new Kind[]{Kind.SLAM, Kind.VERTICAL};
            default -> null;
        };
    }

    private static List<String> pool(ItemStack stack) {
        List<String> pool = new ArrayList<>();
        for (String id : WeaponProfiles.animations(stack)) {
            addIfPresent(pool, id);
        }
        for (String id : specializedCategory(stack)) {
            addIfPresent(pool, id);
        }
        return pool;
    }

    private static List<String> specializedCategory(ItemStack stack) {
        List<String> animations = AbilityConfigs.categoryAnimations(BetterCombatBridge.category(stack));
        for (String id : animations) {
            if (!id.startsWith("bettercombat:")) {
                return animations;
            }
        }
        return List.of();
    }

    private static void addIfPresent(List<String> pool, String id) {
        if (id == null || id.isBlank() || pool.contains(id)) {
            return;
        }
        if (BetterCombatBridge.animation(id) != null) {
            pool.add(id);
        }
    }

    private static List<String> ofKind(List<String> source, Kind kind) {
        List<String> out = new ArrayList<>();
        for (String id : source) {
            if (kind(id) == kind) {
                out.add(id);
            }
        }
        return out;
    }

    private static Kind kind(String id) {
        String value = id.toLowerCase(Locale.ROOT);
        if (value.contains("spin") || value.contains("whirl")) {
            return Kind.SPIN;
        }
        if (value.contains("slam") || value.contains("smash") || value.contains("overhead")) {
            return Kind.SLAM;
        }
        if (value.contains("upward") || value.contains("uppercut") || value.contains("rise")) {
            return Kind.RISING;
        }
        if (value.contains("stab") || value.contains("thrust") || value.contains("throw")
                || value.contains("punch") || value.contains("shoot") || value.contains("mounted")) {
            return Kind.STAB;
        }
        if (value.contains("vertical")) {
            return Kind.VERTICAL;
        }
        if (value.contains("slash") || value.contains("swipe") || value.contains("cross")
                || value.contains("switch_blade") || value.contains("horizontal")) {
            return Kind.HORIZONTAL;
        }
        return Kind.OTHER;
    }

    private static List<String> preferredHand(List<String> source, boolean twoHanded) {
        int best = 3;
        for (String id : source) {
            best = Math.min(best, handRank(id, twoHanded));
        }
        List<String> out = new ArrayList<>();
        for (String id : source) {
            if (handRank(id, twoHanded) == best) {
                out.add(id);
            }
        }
        return out;
    }

    private static int handRank(String id, boolean twoHanded) {
        String value = id.toLowerCase(Locale.ROOT);
        boolean one = value.contains("one_handed");
        boolean two = value.contains("two_handed");
        if (twoHanded) {
            return two ? 0 : one ? 2 : 1;
        }
        return one ? 0 : two ? 2 : 1;
    }

    private static List<String> direction(List<String> source, int swipe) {
        List<String> out = new ArrayList<>(source);
        if (swipe == 0) {
            return out;
        }
        String first = swipe >= 0 ? "left" : "right";
        String second = swipe >= 0 ? "right" : "left";
        out.sort((a, b) -> Integer.compare(rank(a, first, second), rank(b, first, second)));
        return out;
    }

    private static int rank(String id, String first, String second) {
        String value = id.toLowerCase(Locale.ROOT);
        if (value.contains(first)) {
            return 0;
        }
        if (value.contains(second)) {
            return 2;
        }
        return 1;
    }

    private enum Kind {
        SPIN, SLAM, RISING, STAB, VERTICAL, HORIZONTAL, OTHER
    }

    @Nullable
    private static String[] pairFor(Archetype archetype, GestureShape shape, boolean twoHanded, int swipe) {
        boolean fromLeft = swipe >= 0;
        return switch (shape) {
            case LINE_H -> archetype == Archetype.SHIELD
                    ? raw("gesturalcombat:shield_bash")
                    : twoHanded
                    ? horizontalPair("two_handed_slash_horizontal_left", "two_handed_slash_horizontal_right", fromLeft)
                    : horizontalPair("one_handed_slash_horizontal_left", "one_handed_slash_horizontal_right", fromLeft);
            case LINE_V_UP -> archetype == Archetype.SHIELD
                    ? raw("gesturalcombat:shield_charge")
                    : archetype == Archetype.BOW
                    ? null
                    : custom(singleUp(archetype));
            case LINE_V_DOWN -> archetype == Archetype.SHIELD || archetype == Archetype.BOW
                    ? null
                    : custom(singleDown(archetype, twoHanded));
            case CIRCLE -> archetype == Archetype.SHIELD
                    ? raw("gesturalcombat:shield_throw")
                    : archetype == Archetype.BOW
                    ? null
                    : prefixed("two_handed_spin");
            case ZIGZAG -> twoHanded
                    ? horizontalPair("two_handed_slash_horizontal_left", "two_handed_slash_horizontal_right", fromLeft)
                    : horizontalPair("one_handed_slash_horizontal_left", "one_handed_swipe_horizontal_right", fromLeft);
            case V -> archetype == Archetype.SWORD ? raw("gesturalcombat:sword_guard") : null;
            case CARET -> switch (archetype) {
                case SWORD -> raw("gesturalcombat:sword_skirmish");
                case AXE -> raw("gesturalcombat:axe_wide");
                default -> null;
            };
            case UNKNOWN -> null;
        };
    }

    private static String[] horizontalPair(String fromLeftName, String fromRightName, boolean fromLeft) {
        return fromLeft ? prefixed(fromLeftName, fromRightName) : prefixed(fromRightName, fromLeftName);
    }

    private static String[] raw(String... names) {
        return names;
    }

    private static String[] custom(String name) {
        return new String[]{"gesturalcombat:" + name};
    }

    private static String[] prefixed(String... names) {
        String[] out = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = "bettercombat:" + names[i];
        }
        return out;
    }

    private static String singleUp(Archetype archetype) {
        return switch (archetype) {
            case SWORD -> "sword_rising";
            case AXE -> "axe_rend";
            case SPEAR -> "spear_thrust";
            case SHIELD -> "shield_charge";
            case BOW -> "sword_rising";
        };
    }

    private static String singleDown(Archetype archetype, boolean twoHanded) {
        String suffix = twoHanded ? "_two_handed" : "";
        return switch (archetype) {
            case SWORD -> "sword_execute" + suffix;
            case AXE -> "axe_cleave" + suffix;
            case SPEAR -> "spear_skyfall" + suffix;
            case SHIELD -> "shield_guard";
            case BOW -> "sword_execute";
        };
    }

    public static String describe(String animationId) {
        return animationId == null ? "none" : animationId.toLowerCase(Locale.ROOT);
    }
}
