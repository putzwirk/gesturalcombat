package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.States;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EdgePairedEffect implements TechniqueEffect {
    private final double range;
    private final double damageMultiplier;
    private final int intervalTicks;
    private final double edgeBonus;
    private final double knockback;

    public EdgePairedEffect(double range, double damageMultiplier, int intervalTicks, double edgeBonus, double knockback) {
        this.range = range;
        this.damageMultiplier = damageMultiplier;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.edgeBonus = edgeBonus;
        this.knockback = knockback;
    }

    @Override
    public int hitCount() {
        return 2;
    }

    @Override
    public int hitIntervalTicks() {
        return intervalTicks;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier);
        pulse(player, level, stack, technique, base, 0, false);
        GestureScheduler.schedule(level, intervalTicks, () -> pulse(player, level, stack, technique, base, 1, true));
    }

    private void pulse(ServerPlayer player, Level level, ItemStack stack, Technique technique, double base, int index, boolean second) {
        if (!player.isAlive() || player.isRemoved() || player.level() != level) {
            return;
        }
        boolean bonus = second && States.alternateEdge(player);
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, Hitbox.sphere(range * 0.8));
        double damage = base * (bonus ? edgeBonus : 1.0);
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, (float) damage, technique, second ? knockback : 0.0, 0.0);
        }
    }
}
