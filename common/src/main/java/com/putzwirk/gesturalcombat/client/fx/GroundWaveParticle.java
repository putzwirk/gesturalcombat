package com.putzwirk.gesturalcombat.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class GroundWaveParticle extends TextureSheetParticle implements FxParticle {
    private final SpriteSet sprites;
    private float finalSize = 3.0f;
    private float baseAlpha = 0.95f;

    protected GroundWaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = 12;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.hasPhysics = false;
        this.quadSize = 0.6f;
        this.lifetime = 12;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void configure(float roll, float spin, float scale, int rgb, int life) {
        this.finalSize = Math.max(0.8f, scale);
        this.lifetime = Math.max(4, life);
        this.quadSize = this.finalSize * 0.25f;
        this.baseAlpha = Mth.clamp(0.35f + 0.65f * (scale / 4.0f), 0.35f, 0.95f);
        this.setColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age < this.lifetime) {
            this.setSpriteFromAge(this.sprites);
        }
        float progress = Mth.clamp(this.age / (float) Math.max(1, this.lifetime), 0.0f, 1.0f);
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        this.quadSize = this.finalSize * (0.18f + 0.82f * eased);
        this.alpha = this.baseAlpha * (1.0f - progress * progress);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 view = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z());
        float size = this.getQuadSize(partialTicks);
        int light = this.getLightColor(partialTicks);
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        buffer.addVertex(px - size, py, pz - size).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(px - size, py, pz + size).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(px + size, py, pz + size).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(px + size, py, pz - size).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
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
        public GroundWaveParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                 double vx, double vy, double vz) {
            return new GroundWaveParticle(level, x, y, z, sprites);
        }
    }
}
