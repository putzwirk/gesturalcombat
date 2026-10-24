package com.putzwirk.gesturalcombat.client.anim;

import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class Animations {
    private Animations() {
    }

    public static void playLocal(String animationId, String hand, float length, float upswing) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            play(player, animationId, hand, length, upswing);
        }
    }

    public static void playFor(int entityId, String animationId, String hand, float length, float upswing) {
        Player player = playerById(entityId);
        if (player != null) {
            play(player, animationId, hand, length, upswing);
        }
    }

    public static void stopLocal() {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            BetterCombatBridge.stopAttackAnimation(player);
        }
    }

    public static void stopFor(int entityId) {
        Player player = playerById(entityId);
        if (player != null) {
            BetterCombatBridge.stopAttackAnimation(player);
        }
    }

    private static void play(Player player, String animationId, String handName, float length, float upswing) {
        if (animationId == null || BetterCombatBridge.animation(animationId) == null) {
            return;
        }
        Object hand = handName == null ? null : BetterCombatBridge.handFromName(handName);
        if (hand == null) {
            ItemStack stack = player.getMainHandItem();
            hand = BetterCombatBridge.hand(stack);
        }
        BetterCombatBridge.playAttackAnimation(player, animationId, hand, length, upswing);
    }

    @Nullable
    private static Player playerById(int entityId) {
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        return entity instanceof Player player ? player : null;
    }
}
