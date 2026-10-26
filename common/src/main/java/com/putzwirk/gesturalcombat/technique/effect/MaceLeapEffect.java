package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class MaceLeapEffect implements TechniqueEffect {
    private final FamilyArcEffect strike;
    private final double jump;

    public MaceLeapEffect(double range, double arcDegrees, double damageMultiplier, double jump, FamilyRule rule) {
        this.strike = new FamilyArcEffect(range, arcDegrees, damageMultiplier, 0.25, 0.55, rule);
        this.jump = jump;
    }

    @Override
    public int windupTicks() {
        return 1;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        strike.execute(player, level, stack, technique);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.45, Math.max(motion.y, jump), motion.z * 0.45);
        player.hurtMarked = true;
        EffectSupport.ringParticles(level, player, 1.4, technique, stack);
    }
}
