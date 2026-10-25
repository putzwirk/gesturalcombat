package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MaceSlamEffect implements TechniqueEffect {
    private static final double CHILL_RADIUS = 3.0;
    private static final int CHILL_TICKS = 80;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;

    public MaceSlamEffect(double range, double arcDegrees, double damageMultiplier) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier);
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, (float) base, technique, 0.6, 0.0);
        }
        for (LivingEntity target : Hitbox.sphere(CHILL_RADIUS).query(player, level)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL_TICKS, 1, false, true, true));
        }
        EffectSupport.ringParticles(level, player, CHILL_RADIUS, technique, stack);
        EffectSupport.groundWave(level, player, technique, stack, CHILL_RADIUS);
        EffectSupport.sound(level, player, SoundEvents.GLASS_BREAK, 0.8f, 0.7f);
    }
}
