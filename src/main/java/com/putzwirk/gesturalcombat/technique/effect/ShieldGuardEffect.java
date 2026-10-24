package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GuardState;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ShieldGuardEffect implements TechniqueEffect {
    private final int durationTicks;

    public ShieldGuardEffect(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        GuardState.begin(player, durationTicks);
        player.startUsingItem(InteractionHand.OFF_HAND);
        EffectSupport.sound(level, player, SoundEvents.SHIELD_BLOCK, 1.0f, 1.0f);
        GestureScheduler.schedule(level, durationTicks, () -> {
            GuardState.end(player);
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
        });
    }
}
