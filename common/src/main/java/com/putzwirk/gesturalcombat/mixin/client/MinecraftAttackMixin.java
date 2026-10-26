package com.putzwirk.gesturalcombat.mixin.client;

import com.putzwirk.gesturalcombat.client.gesture.GestureInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class, priority = 1500)
public class MinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void gesturalcombat$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        if (GestureInput.suppressVanillaAttack()) {
            cir.setReturnValue(false);
        }
    }
}
