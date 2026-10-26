package com.putzwirk.gesturalcombat.client.anim;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.core.Archetype;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import javax.annotation.Nullable;

public final class CameraEffects {
    private static final float MAX_ROLL_DEGREES = 4.5f;
    private static final float YAW_KICK_DEGREES = 1.1f;
    private static final float PITCH_KICK_DEGREES = 0.8f;
    private static final float SHAKE_DEGREES = 0.3f;
    private static final float FOV_PULSE = 0.10f;
    private static final int DURATION = 14;

    private static int remaining;
    private static Archetype archetype;
    private static float weightScale = 1.0f;

    private CameraEffects() {
    }

    public static void pulse(@Nullable Archetype source) {
        pulse(source, 1.0f);
    }

    public static void pulse(@Nullable Archetype source, float scale) {
        archetype = source;
        weightScale = Math.max(0.4f, Math.min(1.8f, scale));
        remaining = DURATION;
    }

        public static void tick() {
        if (remaining > 0) {
            remaining--;
        }
    }

        public static float[] angles() {
        float envelope = envelope();
        if (envelope <= 0.0f) {
            return null;
        }
        float strength = strength(archetype) * weightScale;
        float age = DURATION - remaining + Minecraft.getInstance().getFrameTime();
        float roll = MAX_ROLL_DEGREES * envelope * strength;
        float yaw = YAW_KICK_DEGREES * envelope * strength;
        float pitch = PITCH_KICK_DEGREES * envelope * strength;
        if (age >= 4.0f && age <= 9.0f) {
            float shake = Mth.sin(age * 1.7f) * SHAKE_DEGREES;
            roll += shake;
            pitch += shake * 0.6f;
        }
        return new float[]{yaw, pitch, roll};
    }

        public static float fov(float current) {
        float envelope = envelope();
        if (envelope <= 0.0f) {
            return current;
        }
        return current * (1.0f + FOV_PULSE * envelope);
    }

    private static float envelope() {
        if (remaining <= 0) {
            return 0.0f;
        }
        float t = Mth.clamp((DURATION - remaining + Minecraft.getInstance().getFrameTime()) / DURATION, 0.0f, 1.0f);
        return Mth.sin((float) Math.PI * t);
    }

    private static float strength(@Nullable Archetype source) {
        if (source == null) {
            return 0.6f;
        }
        return switch (source) {
            case AXE -> 1.0f;
            case SWORD -> 0.6f;
            case SPEAR -> 0.55f;
            case SHIELD -> 0.5f;
            case BOW -> 0.35f;
        };
    }
}
