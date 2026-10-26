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

public final class PostureEffect implements TechniqueEffect {
    private static final int HEAVY_POSTURE = 80;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final int posture;
    private final double lift;

    public PostureEffect(double range, double arcDegrees, double damageMultiplier, int posture) {
        this(range, arcDegrees, damageMultiplier, posture, 0.0);
    }

    public PostureEffect(double range, double arcDegrees, double damageMultiplier, int posture, double lift) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.posture = Math.max(1, posture);
        this.lift = lift;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, EffectSupport.defaultHitbox(technique, range, arcDegrees));
        double base = EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier);
        double knockback = AbilityConfigs.knockback(technique, stack, 0.15);
        double knockup = AbilityConfigs.lift(technique, stack, lift);
        boolean heavy = false;
        for (LivingEntity target : hitbox.query(player, level)) {
            boolean staggered = PostureState.get(target) >= HEAVY_POSTURE;
            heavy |= staggered;
            EffectSupport.hit(player, target, (float) (base * (staggered ? 1.6 : 1.0)), technique, knockback, knockup);
            PostureState.add(target, posture);
        }
        if (heavy) {
            EffectSupport.sound(level, player, SoundEvents.PLAYER_ATTACK_CRIT, 0.7f, 0.8f);
        }
    }
}
