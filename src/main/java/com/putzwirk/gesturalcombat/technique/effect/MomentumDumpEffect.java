package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.MomentumState;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MomentumDumpEffect implements TechniqueEffect {
    private static final double BASE_ARC = 200.0;
    private static final double ARC_PER_STACK = 20.0;
    private static final double BASE_DAMAGE = 0.9;
    private static final double DAMAGE_PER_STACK = 0.15;

    private final double range;

    public MomentumDumpEffect(double range) {
        this.range = range;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        int stacks = MomentumState.consume(player);
        double angle = Math.min(360.0, BASE_ARC + ARC_PER_STACK * stacks);
        double base = EffectSupport.damageOf(player, technique, stack) * (BASE_DAMAGE + DAMAGE_PER_STACK * stacks);
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, Hitbox.arc(range, angle));
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, (float) base, technique, 0.6, 0.0);
        }
        EffectSupport.sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
    }
}
