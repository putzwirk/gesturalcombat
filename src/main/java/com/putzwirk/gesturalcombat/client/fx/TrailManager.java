package com.putzwirk.gesturalcombat.client.fx;

import com.putzwirk.gesturalcombat.Config;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TrailManager {
    private static final int MAX_SAMPLES = 64;
    public static final long FADE_MS = 300L;
    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private TrailManager() {
    }

    public static void start(int playerId, int lifeTicks, int tint, boolean offhand) {
        if (!Config.INSTANCE.weaponTrails.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        long expires = now + Math.max(120L, lifeTicks * 50L) + FADE_MS;
        Trail trail = TRAILS.computeIfAbsent(playerId, id -> new Trail());
        trail.expiresAt = expires;
        trail.tint = tint;
        trail.offhand = offhand;
    }

    public static boolean active(int playerId, boolean offhand) {
        Trail trail = TRAILS.get(playerId);
        return trail != null && trail.offhand == offhand && System.currentTimeMillis() <= trail.expiresAt;
    }

    public static void sample(int playerId, Vec3 base, Vec3 tip) {
        Trail trail = TRAILS.get(playerId);
        if (trail == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now > trail.expiresAt) {
            return;
        }
        trail.samples.addLast(new Sample(now, base, tip));
        while (trail.samples.size() > MAX_SAMPLES) {
            trail.samples.removeFirst();
        }
    }

    public static void clear() {
        TRAILS.clear();
    }

    static Collection<Trail> trails() {
        return TRAILS.values();
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Trail> iterator = TRAILS.values().iterator();
        while (iterator.hasNext()) {
            Trail trail = iterator.next();
            if (now > trail.expiresAt + FADE_MS) {
                iterator.remove();
                continue;
            }
            while (!trail.samples.isEmpty() && now - trail.samples.peekFirst().time() > FADE_MS) {
                trail.samples.removeFirst();
            }
        }
    }

    public record Sample(long time, Vec3 base, Vec3 tip) {
    }

    static final class Trail {
        final Deque<Sample> samples = new ArrayDeque<>();
        long expiresAt;
        int tint = 0xFFFFFF;
        boolean offhand;
    }
}
