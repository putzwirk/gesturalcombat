package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RampFlurryEffect implements TechniqueEffect {
    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final int hits;
    private final int startInterval;
    private final int endInterval;
    private final double knockback;
    private final double lift;
    private final FamilyRule rule;

    public RampFlurryEffect(double range, double arcDegrees, double damageMultiplier, int hits, int intervalTicks,
                            int endIntervalTicks, double knockback, double lift, FamilyRule rule) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.hits = Math.max(1, hits);
        this.startInterval = Math.max(1, intervalTicks);
        this.endInterval = Math.max(1, endIntervalTicks);
        this.knockback = knockback;
        this.lift = lift;
        this.rule = rule;
    }

    @Override
    public int hitCount() {
        return hits;
    }

    @Override
    public int hitIntervalTicks() {
        return startInterval;
    }

    @Override
    public int hitIntervalEndTicks() {
        return endInterval;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        int[] delays = AbilityConfigs.hitDelays(technique, stack, hits, startInterval, endInterval);
        int count = delays.length;
        float per = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier) / count);
        for (int i = 0; i < count; i++) {
            int index = i;
            int delay = delays[i];
            if (delay <= 0) {
                pulse(player, level, stack, technique, per, count, index);
            } else {
                GestureScheduler.schedule(level, delay,
                        () -> pulse(player, level, stack, technique, per, count, index));
            }
        }
    }

    private void pulse(ServerPlayer player, Level level, ItemStack stack, Technique technique, float per, int count, int index) {
        if (!player.isAlive() || player.isRemoved()) {
            return;
        }
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        boolean finisher = index >= count - 1;
        double force = finisher ? AbilityConfigs.knockback(technique, stack, knockback) : 0.0;
        double up = finisher ? AbilityConfigs.lift(technique, stack, lift) : 0.0;
        if (index == 0 && technique.shape() == com.putzwirk.gesturalcombat.core.GestureShape.LINE_V_DOWN
                && com.putzwirk.gesturalcombat.weapon.WeaponRules.heavy(stack)) {
            EffectSupport.groundWave(level, player, technique, stack, hitbox.range() * 0.85);
        }
        for (LivingEntity target : hitbox.query(player, level)) {
            double damage = rule.adjust(player, target, per, index);
            EffectSupport.hit(player, target, (float) Math.max(0.0, damage), technique, force, up);
        }
    }
}
