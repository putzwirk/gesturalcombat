package com.putzwirk.gesturalcombat.compat;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

public final class BetterCombatBridge {
    private static boolean resolved;
    private static Method getAttributes;
    private static Method category;
    private static Method attackRange;
    private static Method twoHanded;
    private static Object mainHand;
    private static Object twoHandedHand;

    private BetterCombatBridge() {
    }

    public static boolean available() {
        resolve();
        return getAttributes != null;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> registry = Class.forName("net.bettercombat.logic.WeaponRegistry");
            getAttributes = registry.getMethod("getAttributes", ItemStack.class);
            Class<?> attributes = Class.forName("net.bettercombat.api.WeaponAttributes");
            category = attributes.getMethod("category");
            attackRange = attributes.getMethod("attackRange");
            twoHanded = attributes.getMethod("isTwoHanded");
            Class<?> hand = Class.forName("net.bettercombat.logic.AnimatedHand");
            for (Object constant : hand.getEnumConstants()) {
                String name = ((Enum<?>) constant).name();
                if (name.equals("MAIN_HAND")) {
                    mainHand = constant;
                } else if (name.equals("TWO_HANDED")) {
                    twoHandedHand = constant;
                }
            }
        } catch (Throwable ignored) {
            getAttributes = null;
        }
    }

    @Nullable
    private static Object attributes(ItemStack stack) {
        resolve();
        if (getAttributes == null || stack.isEmpty()) {
            return null;
        }
        try {
            return getAttributes.invoke(null, stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static String category(ItemStack stack) {
        Object value = attributes(stack);
        if (value == null || category == null) {
            return null;
        }
        try {
            Object result = category.invoke(value);
            return result instanceof String text ? text : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static double attackRange(ItemStack stack) {
        Object value = attributes(stack);
        if (value == null || attackRange == null) {
            return 0.0;
        }
        try {
            Object result = attackRange.invoke(value);
            return result instanceof Number number ? number.doubleValue() : 0.0;
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    public static boolean isTwoHanded(ItemStack stack) {
        Object value = attributes(stack);
        if (value == null || twoHanded == null) {
            return false;
        }
        try {
            Object result = twoHanded.invoke(value);
            return result instanceof Boolean flag && flag;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    public static Object hand(ItemStack stack) {
        return isTwoHanded(stack) ? twoHandedHand : mainHand;
    }

    @Nullable
    public static String handName(ItemStack stack) {
        Object hand = hand(stack);
        return hand instanceof Enum<?> e ? e.name() : null;
    }

    @Nullable
    public static Object handFromName(String name) {
        resolve();
        if (name == null) {
            return null;
        }
        try {
            Class<?> hand = Class.forName("net.bettercombat.logic.AnimatedHand");
            for (Object constant : hand.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals(name)) {
                    return constant;
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    public static void cancelPendingSwing(Object minecraft) {
        try {
            Class<?> api = Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat");
            if (!api.isInstance(minecraft)) {
                return;
            }
            Object ticks = api.getMethod("getUpswingTicks").invoke(minecraft);
            if (ticks instanceof Integer value && value > 0) {
                api.getMethod("cancelUpswing").invoke(minecraft);
            }
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    public static String family(@Nullable String categoryValue) {
        if (categoryValue == null) {
            return null;
        }
        String value = categoryValue.toLowerCase(Locale.ROOT);
        if (matches(value, "spear", "lance", "pike", "halberd", "glaive", "polearm", "trident", "naginata", "partisan", "harpoon", "javelin", "staff")) {
            return "spear";
        }
        if (matches(value, "axe", "hatchet", "cleaver", "scythe", "poleaxe", "greataxe")) {
            return "axe";
        }
        if (matches(value, "sword", "blade", "katana", "saber", "sabre", "rapier", "cutlass", "dagger", "sickle", "scimitar", "claymore", "machete", "falchion", "longsword")) {
            return "sword";
        }
        return null;
    }

    public static boolean isMiningWithWeaponsEnabled() {
        try {
            Class<?> client = Class.forName("net.bettercombat.client.BetterCombatClient");
            Object config = client.getField("config").get(null);
            if (config == null) {
                return false;
            }
            Object value = config.getClass().getField("isMiningWithWeaponsEnabled").get(config);
            return value instanceof Boolean enabled && enabled;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean matches(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static Object animation(String name) {
        if (name == null) {
            return null;
        }
        try {
            Class<?> registry = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry");
            Method get = registry.getMethod("getAnimation", net.minecraft.resources.ResourceLocation.class);
            Object animation = get.invoke(null, net.minecraft.resources.ResourceLocation.parse(name));
            if (animation != null) {
                return animation;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> registry = Class.forName("net.bettercombat.client.animation.AnimationRegistry");
            Object map = registry.getField("animations").get(null);
            if (map instanceof Map<?, ?> animations) {
                return ((Map<String, Object>) animations).get(name);
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    public static int animationCount(String namespace) {
        try {
            Class<?> registry = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry");
            Object map = registry.getMethod("getModAnimations", String.class).invoke(null, namespace);
            return map instanceof Map<?, ?> animations ? animations.size() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean playAttackAnimation(Object player, String animationId, Object hand, float lengthTicks, float upswing) {
        if (player == null || animationId == null) {
            return false;
        }
        try {
            Class<?> animatable = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable");
            if (!animatable.isInstance(player)) {
                return false;
            }
            Method play = animatable.getMethod("playAttackAnimation", String.class, Class.forName("net.bettercombat.logic.AnimatedHand"), float.class, float.class);
            play.invoke(player, animationId, hand, lengthTicks, upswing);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean stopAttackAnimation(Object player) {
        if (player == null) {
            return false;
        }
        try {
            Class<?> animatable = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable");
            if (!animatable.isInstance(player)) {
                return false;
            }
            Method stop = animatable.getMethod("stopAttackAnimation", float.class);
            stop.invoke(player, 5.0f);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
