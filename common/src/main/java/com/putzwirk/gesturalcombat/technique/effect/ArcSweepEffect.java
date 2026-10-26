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

public final class ArcSweepEffect implements TechniqueEffect {
    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final double liftPower;
    private final double knockback;

    public ArcSweepEffect(double range, double arcDegrees, float damageMultiplier, double liftPower, double knockback) {
        this(range, arcDegrees, (double) damageMultiplier, liftPower, knockback);
    }

    public ArcSweepEffect(double range, double arcDegrees, double damageMultiplier, double liftPower, double knockback) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.liftPower = liftPower;
        this.knockback = knockback;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier));
        double force = AbilityConfigs.knockback(technique, stack, knockback);
        double lift = AbilityConfigs.lift(technique, stack, liftPower);
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, damage, technique, force, lift);
        }
        if (damageMultiplier >= 1.5) {
            EffectSupport.sound(level, player, SoundEvents.PLAYER_ATTACK_CRIT, 0.7f, 0.7f);
        }
    }
}
