package com.putzwirk.gesturalcombat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.putzwirk.gesturalcombat.client.fx.TrailCapture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererCaptureMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void gesturalcombat$sampleBlade(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand,
                                            PoseStack poseStack, MultiBufferSource buffer, int light, int overlay,
                                            BakedModel model, CallbackInfo ci) {
        TrailCapture.sample(itemStack, displayContext, leftHand, poseStack, model);
    }
}
