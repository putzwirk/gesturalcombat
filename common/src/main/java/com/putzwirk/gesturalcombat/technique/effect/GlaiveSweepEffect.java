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

public final class GlaiveSweepEffect implements TechniqueEffect {
    private static final double SWEET_SPOT = 2.4;
    private static final double DEAD_ZONE = 1.6;
    private static final double TIP_MULTIPLIER = 1.35;
    private static final double DEAD_ZONE_MULTIPLIER = 0.75;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;

    public GlaiveSweepEffect(double range, double arcDegrees, double damageMultiplier) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier);
        for (LivingEntity target : hitbox.query(player, level)) {
            double distance = flatDistance(player, target);
            double multiplier = distance > SWEET_SPOT ? TIP_MULTIPLIER : distance < DEAD_ZONE ? DEAD_ZONE_MULTIPLIER : 1.0;
            EffectSupport.hit(player, target, (float) (base * multiplier), technique, 0.0, 0.0);
            if (distance > SWEET_SPOT && target.isAlive()) {
                Vec3 pull = new Vec3(player.getX() - target.getX(), 0.0, player.getZ() - target.getZ());
                if (pull.lengthSqr() > 1.0e-4) {
                    pull = pull.normalize().scale(0.35);
                    target.setDeltaMovement(pull.x, target.getDeltaMovement().y, pull.z);
                    target.hurtMarked = true;
                }
            }
        }
    }

    private static double flatDistance(ServerPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dz * dz) - target.getBbWidth() * 0.5;
    }
}
