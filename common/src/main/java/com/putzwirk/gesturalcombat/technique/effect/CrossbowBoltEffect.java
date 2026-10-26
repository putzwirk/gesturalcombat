package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CrossbowBoltEffect implements TechniqueEffect {
    private static final float BASE_DAMAGE = 9.0f;
    private static final float SPEED = 4.2f;
    private static final byte PIERCE = 6;
    private static final int KNOCKBACK = 5;

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Vec3 look = player.getLookAngle();
        Arrow bolt = new Arrow(level, player);
        bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        bolt.shoot(look.x, look.y, look.z, SPEED, 0.0f);
        bolt.setBaseDamage(EffectSupport.projectileDamage(player, technique, BASE_DAMAGE));
        bolt.setCritArrow(true);
        bolt.setPierceLevel(PIERCE);
        bolt.setKnockback(KNOCKBACK);
        bolt.setShotFromCrossbow(true);
        level.addFreshEntity(bolt);
        player.stopUsingItem();
        EffectSupport.sound(level, player, SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.85f);
        EffectSupport.sound(level, player, SoundEvents.CROSSBOW_LOADING_END, 0.6f, 1.2f);
    }
}
