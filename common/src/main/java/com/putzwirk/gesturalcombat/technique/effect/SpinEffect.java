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

public final class SpinEffect implements TechniqueEffect {
    private final double radius;
    private final double damageMultiplier;
    private final int hits;
    private final int intervalTicks;

    public SpinEffect(double radius, float damageMultiplier, int hits, int intervalTicks) {
        this(radius, (double) damageMultiplier, hits, intervalTicks);
    }

    public SpinEffect(double radius, double damageMultiplier, int hits, int intervalTicks) {
        this.radius = radius;
        this.damageMultiplier = damageMultiplier;
        this.hits = Math.max(1, hits);
        this.intervalTicks = Math.max(1, intervalTicks);
    }

    @Override
    public int hitCount() {
        return hits;
    }

    @Override
    public int hitIntervalTicks() {
        return intervalTicks;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        int count = AbilityConfigs.hits(technique, stack, hits);
        int interval = AbilityConfigs.hitInterval(technique, stack, intervalTicks);
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier) / count);
        pulse(player, level, stack, technique, damage, count, 0);
        for (int i = 1; i < count; i++) {
            int index = i;
            GestureScheduler.schedule(level, i * Math.max(1, interval), () -> pulse(player, level, stack, technique, damage, count, index));
        }
    }

    private void pulse(ServerPlayer player, Level level, ItemStack stack, Technique technique, float damage, int count, int index) {
        if (!player.isAlive() || player.isRemoved() || player.level() != level) {
            return;
        }
        boolean finisher = index >= count - 1;
        EffectSupport.swingSound(level, player, technique);
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, Hitbox.sphere(radius));
        double force = AbilityConfigs.knockback(technique, stack, finisher ? 0.45 : 0.0);
        double lift = AbilityConfigs.lift(technique, stack, finisher ? 0.05 : 0.0);
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, damage, technique, force, lift, !finisher);
        }
    }
}
