package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.PostureState;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PostureFlurryEffect implements TechniqueEffect {
    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final int hits;
    private final int intervalTicks;
    private final int posture;

    public PostureFlurryEffect(double range, double arcDegrees, double damageMultiplier, int hits, int intervalTicks, int posture) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.hits = Math.max(1, hits);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.posture = Math.max(1, posture);
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
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        double knockback = index == count - 1 ? AbilityConfigs.knockback(technique, stack, 0.3) : 0.0;
        for (LivingEntity target : hitbox.query(player, level)) {
            float applied = PostureState.isStaggered(target) ? damage * 1.5f : damage;
            EffectSupport.hit(player, target, applied, technique, knockback, 0.0);
            PostureState.add(target, posture);
        }
    }
}
