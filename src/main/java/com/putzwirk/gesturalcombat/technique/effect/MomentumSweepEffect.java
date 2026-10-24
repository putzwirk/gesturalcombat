package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.MomentumState;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MomentumSweepEffect implements TechniqueEffect {
    private static final double PER_STACK = 0.06;
    private static final int MAX_GAIN = 3;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;

    public MomentumSweepEffect(double range, double arcDegrees, double damageMultiplier) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        int stacks = MomentumState.get(player);
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier)
                * (1.0 + PER_STACK * stacks);
        double knockback = AbilityConfigs.knockback(technique, stack, 0.35);
        int hits = 0;
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, (float) base, technique, knockback, 0.0);
            hits++;
        }
        if (hits > 0) {
            MomentumState.add(player, Math.min(MAX_GAIN, hits));
        }
    }
}
