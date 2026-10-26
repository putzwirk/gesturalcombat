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
    private static Vector3f FORWARDS;
    @Shadow
    private static Vector3f UP;
    @Shadow
    private static Vector3f LEFT;
    @Shadow
    private float yRot;
    @Shadow
    private float xRot;
    @Shadow
    private final Quaternionf rotation = new Quaternionf();
    @Shadow
    private final Vector3f forwards = new Vector3f();
    @Shadow
    private final Vector3f up = new Vector3f();
    @Shadow
    private final Vector3f left = new Vector3f();

    @Inject(method = "setup", at = @At("RETURN"))
    private void gesturalcombat$applyCameraAngles(CallbackInfo ci) {
        float[] angles = CameraEffects.angles();
        if (angles == null) {
            return;
        }
        yRot += angles[0];
        xRot = Mth.clamp(xRot + angles[1], -90.0f, 90.0f);
        rotation.rotationYXZ((float) Math.PI - yRot * ((float) Math.PI / 180.0f),
                -xRot * ((float) Math.PI / 180.0f), angles[2] * ((float) Math.PI / 180.0f));
        forwards.set(FORWARDS).rotate(rotation);
        up.set(UP).rotate(rotation);
        left.set(LEFT).rotate(rotation);
    }
}
