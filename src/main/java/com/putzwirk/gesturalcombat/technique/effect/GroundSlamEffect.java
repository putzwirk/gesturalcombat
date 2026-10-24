package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GroundSlamEffect implements TechniqueEffect {
    private final double radius;
    private final double damageMultiplier;
    private final double liftPower;

    public GroundSlamEffect(double radius, float damageMultiplier, double liftPower) {
        this(radius, (double) damageMultiplier, liftPower);
    }

    public GroundSlamEffect(double radius, double damageMultiplier, double liftPower) {
        this.radius = radius;
        this.damageMultiplier = damageMultiplier;
        this.liftPower = liftPower;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, Hitbox.sphere(radius));
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier));
        double force = AbilityConfigs.knockback(technique, stack, 1.2);
        double lift = AbilityConfigs.lift(technique, stack, liftPower);
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, damage, technique, force, lift);
        }
        EffectSupport.ringParticles(level, player, hitbox.range(), technique, stack);
        EffectSupport.sound(level, player, SoundEvents.GENERIC_EXPLODE, 0.6f, 0.8f);
    }
}
