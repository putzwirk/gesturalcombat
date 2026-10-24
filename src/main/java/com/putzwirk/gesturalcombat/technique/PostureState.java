package com.putzwirk.gesturalcombat.technique;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "gesturalcombat")
public final class PostureState {
    private static final int MAX = 100;
    private static final int DECAY_TICKS = 20;
    private static final int DECAY_AMOUNT = 2;
    private static final int STAGGER_WINDOW = 50;
    private static final int STAGGER_PIN = 26;
    private static final Map<UUID, Posture> POSTURE = new HashMap<>();

    private PostureState() {
    }

    public static int get(LivingEntity target) {
        Posture posture = POSTURE.get(target.getUUID());
        return posture == null ? 0 : posture.value;
    }

    public static boolean isStaggered(LivingEntity target) {
        Posture posture = POSTURE.get(target.getUUID());
        return posture != null && target.level().getGameTime() < posture.staggerUntil;
    }

    public static void add(LivingEntity target, int amount) {
        if (target.level().isClientSide() || amount <= 0) {
            return;
        }
        long now = target.level().getGameTime();
        Posture posture = POSTURE.computeIfAbsent(target.getUUID(), id -> new Posture(now));
        posture.value = Math.min(MAX, posture.value + amount);
        posture.lastChange = now;
        if (posture.value >= MAX) {
            posture.value = 0;
            posture.staggerUntil = now + STAGGER_WINDOW;
            HitStun.pin(target, STAGGER_PIN, true);
        }
    }

    public static void clear(LivingEntity target) {
        POSTURE.remove(target.getUUID());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || POSTURE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Posture>> iterator = POSTURE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Posture> entry = iterator.next();
            Posture posture = entry.getValue();
            Entity entity = findEntity(server, entry.getKey());
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                iterator.remove();
                continue;
            }
            long now = entity.level().getGameTime();
            if (now < posture.staggerUntil) {
                continue;
            }
            if (posture.value <= 0) {
                iterator.remove();
                continue;
            }
            if (now - posture.lastChange < DECAY_TICKS) {
                continue;
            }
            posture.value = Math.max(0, posture.value - DECAY_AMOUNT);
            posture.lastChange = now;
        }
    }

    private static Entity findEntity(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static final class Posture {
        int value;
        long lastChange;
        long staggerUntil;

        Posture(long now) {
            this.lastChange = now;
        }
    }
}
