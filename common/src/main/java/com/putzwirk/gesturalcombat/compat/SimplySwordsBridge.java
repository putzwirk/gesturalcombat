package com.putzwirk.gesturalcombat.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public final class SimplySwordsBridge {
    private static boolean resolved;
    private static Method delegatedHit;

    private SimplySwordsBridge() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> api = Class.forName("net.sweenus.simplyswords.api.SimplySwordsAPI");
            delegatedHit = api.getMethod("applyDelegatedWeaponHit", ItemStack.class, LivingEntity.class,
                    LivingEntity.class, LivingEntity.class, float.class);
        } catch (Throwable ignored) {
            delegatedHit = null;
        }
    }

    public static boolean available() {
        resolve();
        return delegatedHit != null;
    }

    public static boolean applyHit(ItemStack stack, LivingEntity target, ServerPlayer player, float damage) {
        resolve();
        if (delegatedHit == null || stack.isEmpty() || target == null || !target.isAlive()) {
            return false;
        }
        try {
            Object result = delegatedHit.invoke(null, stack, target, player, player, damage);
            return result instanceof Boolean value && value;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
