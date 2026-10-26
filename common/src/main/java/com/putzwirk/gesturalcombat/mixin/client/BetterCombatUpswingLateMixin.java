package com.putzwirk.gesturalcombat.mixin.client;

import com.putzwirk.gesturalcombat.client.gesture.GestureInput;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class, priority = 1500)
public class BetterCombatUpswingLateMixin {
    @Inject(method = "startUpswing", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void gesturalcombat$blockSwingLate(WeaponAttributes attributes, CallbackInfo ci) {
        if (GestureInput.suppressVanillaAttack()) {
            ci.cancel();
        }
    }

    @Inject(method = "performAttack", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void gesturalcombat$blockDamageLate(CallbackInfo ci) {
        if (GestureInput.suppressVanillaAttack()) {
            ci.cancel();
        }
    }

    @Inject(method = "pre_doAttack", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void gesturalcombat$blockClickLate(CallbackInfoReturnable<Boolean> cir) {
        if (GestureInput.suppressVanillaAttack()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
