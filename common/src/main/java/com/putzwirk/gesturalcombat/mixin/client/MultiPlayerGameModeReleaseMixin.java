package com.putzwirk.gesturalcombat.mixin.client;

import com.putzwirk.gesturalcombat.client.ClientState;
import com.putzwirk.gesturalcombat.client.gesture.GestureInput;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeReleaseMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void gesturalcombat$holdShieldGuard(Player player, CallbackInfo ci) {
        if (ClientState.guardTicks > 0) {
            ci.cancel();
            return;
        }
        if (GestureInput.consumeVanillaRelease()) {
            ci.cancel();
            return;
        }
        if (GestureInput.finishBowRelease()) {
            ci.cancel();
        }
    }
}
