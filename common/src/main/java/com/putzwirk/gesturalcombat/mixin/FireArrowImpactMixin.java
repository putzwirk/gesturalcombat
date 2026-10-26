package com.putzwirk.gesturalcombat.mixin;

import com.putzwirk.gesturalcombat.technique.FireArrowEvents;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class FireArrowImpactMixin {
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void gesturalcombat$fireArrowImpact(HitResult result, CallbackInfo ci) {
        if ((Object) this instanceof AbstractArrow arrow && FireArrowEvents.impact(arrow, result)) {
            ci.cancel();
        }
    }
}
