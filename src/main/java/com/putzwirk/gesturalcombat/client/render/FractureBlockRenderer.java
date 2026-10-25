package com.putzwirk.gesturalcombat.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.putzwirk.gesturalcombat.world.FractureBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class FractureBlockRenderer implements BlockEntityRenderer<FractureBlockEntity> {
    public FractureBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FractureBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = entity.original();
        if (state.isAir()) {
            return;
        }
        float rise = Math.min(1.0F, entity.age() / 5.0F);
        float fall = Math.min(1.0F, (entity.life() - entity.age()) / 7.0F);
        float height = entity.lift() * Math.max(0.0F, Math.min(rise, fall));
        pose.pushPose();
        pose.translate(0.5, -1.0 + height, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(entity.yaw()));
        pose.mulPose(Axis.XP.rotationDegrees(entity.tiltX()));
        pose.mulPose(Axis.ZP.rotationDegrees(entity.tiltZ()));
        pose.translate(-0.5, 0.0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, overlay,
                net.minecraftforge.client.model.data.ModelData.EMPTY, null);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(FractureBlockEntity entity) {
        return false;
    }
}
