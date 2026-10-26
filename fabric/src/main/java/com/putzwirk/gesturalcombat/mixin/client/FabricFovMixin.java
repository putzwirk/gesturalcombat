package com.putzwirk.gesturalcombat.mixin.client;

import com.putzwirk.gesturalcombat.client.anim.CameraEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class FabricFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void gesturalcombat$fovPulse(CallbackInfoReturnable<Float> cir) {
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        cir.setReturnValue(CameraEffects.fov(cir.getReturnValueF()));
    }
}
