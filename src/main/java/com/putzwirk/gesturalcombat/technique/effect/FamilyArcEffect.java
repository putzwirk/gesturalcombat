package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FamilyArcEffect implements TechniqueEffect {
    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final double knockback;
    private final double lift;
    private final FamilyRule rule;

    public FamilyArcEffect(double range, double arcDegrees, double damageMultiplier, double knockback, double lift, FamilyRule rule) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.knockback = knockback;
        this.lift = lift;
        this.rule = rule;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier);
        double force = AbilityConfigs.knockback(technique, stack, knockback);
        double up = AbilityConfigs.lift(technique, stack, lift);
        for (LivingEntity target : hitbox.query(player, level)) {
            double damage = rule.adjust(player, target, base, 0);
            EffectSupport.hit(player, target, (float) Math.max(0.0, damage), technique, force, up);
        }
    }
}
