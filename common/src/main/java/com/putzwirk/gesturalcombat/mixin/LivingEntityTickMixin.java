package com.putzwirk.gesturalcombat.mixin;

import com.putzwirk.gesturalcombat.technique.HitStun;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void gesturalcombat$hitStunTick(CallbackInfo ci) {
        HitStun.tick((LivingEntity) (Object) this);
    }
}
