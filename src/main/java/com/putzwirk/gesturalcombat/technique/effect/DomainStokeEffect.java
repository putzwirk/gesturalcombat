package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.States;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DomainStokeEffect implements TechniqueEffect {
    private static final int COST = 4;

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        int spent = States.spendDomain(player, COST);
        if (spent < COST) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, false, true, true));
        EffectSupport.sound(level, player, SoundEvents.BLAZE_SHOOT, 0.9f, 0.7f);
    }
}
