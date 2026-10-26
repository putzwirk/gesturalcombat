package com.putzwirk.gesturalcombat.mixin.client;

import com.putzwirk.gesturalcombat.client.gesture.GestureInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 1500)
public class MinecraftContinueAttackMixin {
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void gesturalcombat$blockHeldAttack(boolean attacking, CallbackInfo ci) {
        if (!attacking) {
            return;
        }
        if (GestureInput.isDraggingGesture()) {
            ci.cancel();
            return;
        }
        if (GestureInput.suppressVanillaAttack()) {
            ci.cancel();
        }
    }
}
