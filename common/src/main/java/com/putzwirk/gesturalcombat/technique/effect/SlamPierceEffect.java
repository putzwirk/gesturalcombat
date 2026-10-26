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

public final class SlamPierceEffect implements TechniqueEffect {
    private final double slamRange;
    private final double slamArc;
    private final double pierceLength;
    private final double pierceWidth;
    private final double slamShare;
    private final double pierceShare;
    private final double pierceForce;
    private final FamilyRule rule;

    public SlamPierceEffect(double slamRange, double slamArc, double pierceLength, double pierceWidth,
                            double slamShare, double pierceShare, double pierceForce, FamilyRule rule) {
        this.slamRange = slamRange;
        this.slamArc = slamArc;
        this.pierceLength = pierceLength;
        this.pierceWidth = pierceWidth;
        this.slamShare = slamShare;
        this.pierceShare = pierceShare;
        this.pierceForce = pierceForce;
        this.rule = rule;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        double total = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, slamShare + pierceShare);
        Hitbox slam = EffectSupport.hitboxOf(player, technique, stack,
                EffectSupport.defaultHitbox(technique, slamRange, slamArc));
        strike(player, level, stack, technique, slam, (float) (total * slamShare), 0.0, 0);
        EffectSupport.groundWave(level, player, technique, stack, slam.range() * 0.85);
        int delay = Math.max(3, AbilityConfigs.hitInterval(technique, stack, 6));
        Hitbox thrust = Hitbox.box(pierceLength, pierceWidth, 1.8).withLineOfSight(true);
        GestureScheduler.schedule(level, delay, () -> {
            if (!player.isAlive() || player.isRemoved()) {
                return;
            }
            strike(player, level, stack, technique, thrust, (float) (total * pierceShare), pierceForce, 1);
        });
    }

    private void strike(ServerPlayer player, Level level, ItemStack stack, Technique technique, Hitbox hitbox,
                        float damage, double force, int index) {
        for (LivingEntity target : hitbox.query(player, level)) {
            double dealt = rule == null ? damage : rule.adjust(player, target, damage, index);
            EffectSupport.hit(player, target, (float) Math.max(0.0, dealt), technique, force, 0.0);
        }
    }
}
