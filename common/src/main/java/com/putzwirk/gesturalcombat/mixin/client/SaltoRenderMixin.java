package com.putzwirk.gesturalcombat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.putzwirk.gesturalcombat.client.fx.SaltoRotation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class SaltoRenderMixin {
    @Inject(method = "setupRotations", at = @At("RETURN"))
    private void gesturalcombat$flipBody(LivingEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                         float partialTicks, float scale, CallbackInfo ci) {
        if (!(entity instanceof Player)) {
            return;
        }
        float angle = SaltoRotation.angle(entity.getId());
        if (angle <= 0.0f) {
            return;
        }
        float hips = SaltoRotation.hips();
        poseStack.translate(0.0f, hips, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));
        poseStack.translate(0.0f, -hips, 0.0f);
    }
}
