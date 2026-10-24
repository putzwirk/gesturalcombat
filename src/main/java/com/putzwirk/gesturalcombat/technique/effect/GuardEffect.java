package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GuardEffect implements TechniqueEffect {
    private final int durationTicks;
    private final int amplifier;

    public GuardEffect(int durationTicks, int amplifier) {
        this.durationTicks = Math.max(1, durationTicks);
        this.amplifier = Math.max(0, amplifier);
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, amplifier, false, false, true));
        EffectSupport.sound(level, player, SoundEvents.SHIELD_BLOCK, 0.9f, 1.0f);
    }
}
