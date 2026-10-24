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
import net.minecraft.world.phys.Vec3;

public final class LinePierceEffect implements TechniqueEffect {
    private final double length;
    private final double halfWidth;
    private final double damageMultiplier;
    private final double dashPower;

    public LinePierceEffect(double length, double halfWidth, float damageMultiplier, double dashPower) {
        this(length, halfWidth, (double) damageMultiplier, dashPower);
    }

    public LinePierceEffect(double length, double halfWidth, double damageMultiplier, double dashPower) {
        this.length = length;
        this.halfWidth = halfWidth;
        this.damageMultiplier = damageMultiplier;
        this.dashPower = dashPower;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack,
                Hitbox.box(length, halfWidth * 2.0, 1.4).withLineOfSight(true));
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier));
        double force = AbilityConfigs.knockback(technique, stack, 0.15);
        double lift = AbilityConfigs.lift(technique, stack, 0.0);
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, damage, technique, force, lift);
        }
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.0, look.z);
        if (dash.lengthSqr() > 1.0E-4) {
            dash = dash.normalize().scale(dashPower);
            player.push(dash.x, 0.0, dash.z);
            player.hurtMarked = true;
        }
    }
}
