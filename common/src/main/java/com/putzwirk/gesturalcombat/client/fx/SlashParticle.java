package com.putzwirk.gesturalcombat.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class SlashParticle extends TextureSheetParticle implements FxParticle {
    private final SpriteSet sprites;
    private float spin;
    private float fade = 0.45f;

    protected SlashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.setSize(1.0f, 1.0f);
        this.lifetime = 8;
        this.gravity = 0.0f;
        this.friction = 0.92f;
        this.hasPhysics = false;
        this.quadSize = 0.9f;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void configure(float roll, float spin, float scale, int rgb, int life) {
        this.roll = roll;
        this.oRoll = roll;
        this.spin = spin;
        this.quadSize = 0.9f * scale;
        this.lifetime = Math.max(3, life);
        this.fade = spin != 0.0f ? 0.25f : 0.45f;
        this.setColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.spin;
        if (this.age < this.lifetime) {
            this.setSpriteFromAge(this.sprites);
        }
        float progress = this.age / (float) Math.max(1, this.lifetime);
        this.alpha = Math.max(0.0f, 1.0f - progress * this.fade * 2.0f);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new SlashParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
