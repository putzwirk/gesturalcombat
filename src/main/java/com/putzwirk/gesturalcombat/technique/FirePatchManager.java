package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID)
public final class FirePatchManager {
    public static final String TAG = "gesturalcombat_fire_arrow";
    private static final List<Patch> PATCHES = new ArrayList<>();

    private FirePatchManager() {
    }

    public static void spawn(ServerLevel level, Vec3 pos, int durationTicks, double radius, float damage) {
        long now = level.getGameTime();
        PATCHES.add(new Patch(level, pos, now + Math.max(1, durationTicks), now + 20, Math.max(0.5, radius), damage));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PATCHES.isEmpty()) {
            return;
        }
        Iterator<Patch> iterator = PATCHES.iterator();
        while (iterator.hasNext()) {
            Patch patch = iterator.next();
            if (patch.level.getServer() == null) {
                iterator.remove();
                continue;
            }
            long now = patch.level.getGameTime();
            if (now > patch.until) {
                patch.level.sendParticles(ParticleTypes.LARGE_SMOKE, patch.pos.x, patch.pos.y + 0.3, patch.pos.z,
                        8, 0.5, 0.2, 0.5, 0.02);
                iterator.remove();
                continue;
            }
            for (int i = 0; i < 4; i++) {
                double angle = patch.level.random.nextDouble() * Math.PI * 2.0;
                double distance = patch.level.random.nextDouble() * patch.radius;
                double x = patch.pos.x + Math.cos(angle) * distance;
                double z = patch.pos.z + Math.sin(angle) * distance;
                patch.level.sendParticles(ParticleTypes.FLAME, x, patch.pos.y + 0.1, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (now >= patch.nextDamage) {
                patch.nextDamage = now + 20;
                AABB area = new AABB(patch.pos, patch.pos).inflate(patch.radius);
                for (LivingEntity entity : patch.level.getEntitiesOfClass(LivingEntity.class, area,
                        e -> e.isAlive() && !e.isSpectator())) {
                    entity.setSecondsOnFire(40);
                    entity.hurt(patch.level.damageSources().inFire(), patch.damage);
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PATCHES.clear();
    }

    private static final class Patch {
        private final ServerLevel level;
        private final Vec3 pos;
        private final long until;
        private long nextDamage;
        private final double radius;
        private final float damage;

        private Patch(ServerLevel level, Vec3 pos, long until, long nextDamage, double radius, float damage) {
            this.level = level;
            this.pos = pos;
            this.until = until;
            this.nextDamage = nextDamage;
            this.radius = radius;
            this.damage = damage;
        }
    }
}
