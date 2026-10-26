package com.putzwirk.gesturalcombat.mixin;

import com.putzwirk.gesturalcombat.server.AerialSlam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallMixin {
    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float gesturalcombat$fallMultiplier(float multiplier) {
        if ((Object) this instanceof ServerPlayer player) {
            return AerialSlam.fallMultiplier(player, multiplier);
        }
        return multiplier;
    }
}
