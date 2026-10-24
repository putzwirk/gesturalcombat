package com.putzwirk.gesturalcombat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.putzwirk.gesturalcombat.client.ClientState;
import com.putzwirk.gesturalcombat.client.fx.TrailCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererCaptureMixin {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void gesturalcombat$hideThrown(LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext,
                                           boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
                                           int light, CallbackInfo ci) {
        if (entity == Minecraft.getInstance().player && ClientState.hidden(leftHand)) {
            TrailCapture.end();
            ci.cancel();
        }
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void gesturalcombat$beginCapture(LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext,
                                             boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
                                             int light, CallbackInfo ci) {
        TrailCapture.begin(entity, itemStack, displayContext, leftHand);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void gesturalcombat$endCapture(LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext,
                                           boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
                                           int light, CallbackInfo ci) {
        TrailCapture.end();
    }
}
