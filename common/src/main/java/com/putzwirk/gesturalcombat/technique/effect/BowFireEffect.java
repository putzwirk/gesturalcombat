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
    private static final float BASE_DAMAGE = 5.0f;
    private static final float SPEED = 3.0f;
    private static final float DIVERGENCE = 0.4f;
    private static final int ARROW_FIRE_SECONDS = 20;

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Vec3 look = player.getLookAngle();
        Arrow arrow = new Arrow(level, player, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        arrow.shoot(look.x, look.y, look.z, SPEED, DIVERGENCE);
        arrow.setBaseDamage(EffectSupport.projectileDamage(player, technique, BASE_DAMAGE));
        arrow.setCritArrow(true);
        arrow.igniteForSeconds(ARROW_FIRE_SECONDS);
        arrow.addTag(FirePatchManager.TAG);
        level.addFreshEntity(arrow);
        player.stopUsingItem();
        EffectSupport.sound(level, player, SoundEvents.ARROW_SHOOT, 1.0f, 0.9f);
    }
}
