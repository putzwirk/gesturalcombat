package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.PostureState;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DeathblowEffect implements TechniqueEffect {
    private static final double EXECUTE_HEALTH = 0.35;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;

    public DeathblowEffect(double range, double arcDegrees, double damageMultiplier) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier);
        boolean executed = false;
        for (LivingEntity target : hitbox.query(player, level)) {
            boolean lethal = PostureState.isStaggered(target)
                    || target.getHealth() <= target.getMaxHealth() * (float) EXECUTE_HEALTH;
            if (lethal) {
                executed = true;
                float blow = target.getHealth() + target.getArmorValue() * 2.0f + 20.0f;
                EffectSupport.hit(player, target, blow, technique, 0.0, 0.0, true);
                PostureState.clear(target);
            } else {
                EffectSupport.hit(player, target, (float) base, technique, 0.0, 0.35);
                PostureState.add(target, 20);
            }
        }
        EffectSupport.sound(level, player, executed ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_SWEEP,
                executed ? 1.0f : 0.8f, executed ? 0.6f : 1.1f);
    }
}
