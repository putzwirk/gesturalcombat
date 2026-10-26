package com.putzwirk.gesturalcombat.server;

import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.TwinCastPacket;
import com.putzwirk.gesturalcombat.technique.AbilityConfig;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Fx;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.weapon.DualWield;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TwinEcho {
    private TwinEcho() {
    }

    public static void strike(ServerPlayer player, Level level, boolean castOffhand, Technique technique, int swipe) {
        if (!player.isAlive() || player.isRemoved()) {
            return;
        }
        InteractionHand other = castOffhand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(other);
        if (stack.isEmpty()) {
            return;
        }
        Hitbox base = hitbox(technique, stack);
        Hitbox echo = new Hitbox(base.shape(), base.range() * DualWield.ECHO_REACH, base.angle() * DualWield.ECHO_ARC,
                base.width(), base.height(), base.verticalOffset(), base.forwardOffset(),
                Math.max(1, base.maxTargets() / 2), base.lineOfSight());
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, 1.0) * DualWield.ECHO_DAMAGE);
        for (LivingEntity target : echo.query(player, level)) {
            EffectSupport.hit(player, target, damage, technique, 0.05, 0.0, false);
        }
        Fx.ring(player, technique, stack, player.position().add(0.0, 0.35, 0.0), echo.range() * 0.7);
        ModNetwork.sendToTracking(player,
                new TwinCastPacket(player.getId(), technique.id(), swipe, other == InteractionHand.OFF_HAND));
        stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(other));
    }

    private static Hitbox hitbox(Technique technique, ItemStack stack) {
        AbilityConfig config = AbilityConfigs.effective(technique, stack);
        return config != null && config.hitbox() != null ? config.hitbox() : Hitbox.arc(2.6, 120);
    }
}
