package com.putzwirk.gesturalcombat.mixin;

import com.putzwirk.gesturalcombat.technique.FirePatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void gesturalcombat$harmlessFire(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (FirePatchManager.protects(level, pos)) {
            ci.cancel();
        }
    }
}
