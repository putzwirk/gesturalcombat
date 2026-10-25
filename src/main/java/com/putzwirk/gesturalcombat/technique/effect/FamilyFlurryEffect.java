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

public final class FamilyFlurryEffect implements TechniqueEffect {
    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final int hits;
    private final int intervalTicks;
    private final double knockback;
    private final double lift;
    private final FamilyRule rule;

    public FamilyFlurryEffect(double range, double arcDegrees, double damageMultiplier, int hits, int intervalTicks,
                              double knockback, double lift, FamilyRule rule) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.hits = Math.max(1, hits);
        this.intervalTicks = Math.max(1, intervalTicks);
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
        return intervalTicks;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        int count = AbilityConfigs.hits(technique, stack, hits);
        int interval = AbilityConfigs.hitInterval(technique, stack, intervalTicks);
        float per = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier) / count);
        pulse(player, level, stack, technique, per, count, 0);
        for (int i = 1; i < count; i++) {
            int index = i;
            GestureScheduler.schedule(level, i * Math.max(1, interval), () -> pulse(player, level, stack, technique, per, count, index));
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
        java.util.List<LivingEntity> found = hitbox.query(player, level);
        if (com.putzwirk.gesturalcombat.Config.INSTANCE.debugCasts.get()) {
            com.putzwirk.gesturalcombat.Gesturalcombat.LOGGER.info(
                    "gesturalcombat flurry {} hit={} targets={} range={} angle={} hits={} interval={}",
                    technique.id(), index, found.size(), String.format("%.2f", hitbox.range()),
                    String.format("%.0f", hitbox.angle()), count, intervalTicks);
        }
        for (LivingEntity target : found) {
            double damage = rule.adjust(player, target, per, index);
            EffectSupport.hit(player, target, (float) Math.max(0.0, damage), technique, force, up);
        }
    }
}
