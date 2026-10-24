package com.putzwirk.gesturalcombat.client.render;

import com.mojang.math.Axis;
import com.putzwirk.gesturalcombat.entity.ThrownChakramEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ThrownChakramRenderer extends EntityRenderer<ThrownChakramEntity> {
    public ThrownChakramRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThrownChakramEntity entity, float yaw, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 120.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        poseStack.scale(2.0f, 2.0f, 2.0f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownChakramEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}
