package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.States;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CrossSlashEffect implements TechniqueEffect {
    private final double range;
    private final double linkedMultiplier;
    private final double baseMultiplier;

    public CrossSlashEffect(double range, double linkedMultiplier, double baseMultiplier) {
        this.range = range;
        this.linkedMultiplier = linkedMultiplier;
        this.baseMultiplier = baseMultiplier;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        boolean offhand = stack == player.getOffhandItem();
        boolean linked = States.linkedCast(player, offhand, level.getGameTime());
        double angle = linked ? 150.0 : 110.0;
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, Hitbox.arc(range, angle));
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, linked ? linkedMultiplier : baseMultiplier);
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, (float) base, technique, 0.2, 0.0);
        }
        if (linked) {
            EffectSupport.sound(level, player, SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
        }
    }
}
