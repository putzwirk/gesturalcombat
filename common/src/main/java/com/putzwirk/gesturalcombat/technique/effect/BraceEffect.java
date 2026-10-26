package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GuardState;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BraceEffect implements TechniqueEffect {
    private final int durationTicks;

    public BraceEffect(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        GuardState.begin(player, durationTicks);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 1, false, false, true));
        EffectSupport.sound(level, player, SoundEvents.SHIELD_BLOCK, 0.85f, 0.9f);
        GestureScheduler.schedule(level, durationTicks, () -> {
            if (GuardState.isGuarding(player)) {
                GuardState.end(player);
            }
        });
    }
}
