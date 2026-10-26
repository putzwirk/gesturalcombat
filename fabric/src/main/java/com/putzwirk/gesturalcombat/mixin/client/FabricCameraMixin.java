package com.putzwirk.gesturalcombat.mixin.client;

import com.putzwirk.gesturalcombat.client.anim.CameraEffects;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FabricCameraMixin {
    @Shadow
    private float yRot;
    @Shadow
    private float xRot;
    @Shadow
    private Quaternionf rotation;
    @Shadow
    private Vector3f forwards;
    @Shadow
    private Vector3f up;
    @Shadow
    private Vector3f left;

    @Inject(method = "setup", at = @At("RETURN"))
    private void gesturalcombat$applyCameraAngles(CallbackInfo ci) {
        float[] angles = CameraEffects.angles();
        if (angles == null) {
            return;
        }
        float yaw = yRot + angles[0];
        float pitch = Mth.clamp(xRot + angles[1], -90.0f, 90.0f);
        float roll = angles[2];
        rotation.rotationYXZ(-yaw * ((float) Math.PI / 180.0f), pitch * ((float) Math.PI / 180.0f),
                roll * ((float) Math.PI / 180.0f));
        forwards.set(0.0f, 0.0f, 1.0f).rotate(rotation);
        up.set(0.0f, 1.0f, 0.0f).rotate(rotation);
        left.set(1.0f, 0.0f, 0.0f).rotate(rotation);
    }
}
