package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.FirePatchManager;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import com.putzwirk.gesturalcombat.technique.Technique;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BowFireEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Vec3 look = player.getLookAngle();
        Arrow arrow = new Arrow(level, player);
        arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        arrow.shoot(look.x, look.y, look.z, 3.0f, 0.4f);
        arrow.setBaseDamage(EffectSupport.damageOf(player, technique) * 0.3f);
        arrow.setSecondsOnFire(100);
        arrow.getPersistentData().putBoolean(FirePatchManager.TAG, true);
        level.addFreshEntity(arrow);
        player.stopUsingItem();
        EffectSupport.sound(level, player, SoundEvents.ARROW_SHOOT, 1.0f, 0.9f);
    }
}
