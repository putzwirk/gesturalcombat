package com.putzwirk.gesturalcombat.client.fx;

import com.putzwirk.gesturalcombat.particle.FxType;
import com.putzwirk.gesturalcombat.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;

public final class FxClient {
    private FxClient() {
    }

    public static void spawn(FxType type, double x, double y, double z, float vx, float vy, float vz,
                             float roll, float spin, float scale, int rgb, int life) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Particle particle = mc.particleEngine.createParticle(ModParticles.type(type), x, y, z, vx, vy, vz);
        if (particle instanceof SlashParticle slash) {
            slash.configure(roll, spin, scale, rgb, life);
        }
    }
}
