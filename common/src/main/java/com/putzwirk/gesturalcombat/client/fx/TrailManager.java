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
    public static final long FADE_MS = 170L;
    private static final long FINISH_FADE_MS = 90L;
    private static final double START_SPEED = 0.0008;
    private static final long PENDING_MS = 120L;
    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private TrailManager() {
    }

    public static void start(int playerId, int lifeTicks, int windupTicks, int tint, boolean offhand) {
        if (!Config.INSTANCE.weaponTrails.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        long expires = now + Math.max(120L, lifeTicks * 50L) + FADE_MS;
        Trail trail = TRAILS.computeIfAbsent(playerId, id -> new Trail());
        trail.expiresAt = expires;
        trail.startedAt = now;
        trail.windupMs = Math.max(0, windupTicks) * 50L;
        long windowMs = Math.max(120L, lifeTicks * 50L);
        trail.minFinishMs = Math.max(0L, windupTicks) * 50L + Math.max(60L, (windowMs - trail.windupMs) / 2L);
        trail.tint = tint;
        trail.offhand = offhand;
        trail.finished = false;
        trail.started = false;
        trail.fadeMillis = FADE_MS;
        trail.lastTip = null;
        trail.lastTime = 0L;
        trail.pending = null;
        trail.samples.clear();
    }

    public static boolean active(int playerId, boolean offhand) {
        Trail trail = TRAILS.get(playerId);
        return trail != null && trail.offhand == offhand && System.currentTimeMillis() <= trail.expiresAt;
    }

    public static void sample(int playerId, Vec3 base, Vec3 tip) {
        Trail trail = TRAILS.get(playerId);
        if (trail == null || trail.finished) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now > trail.expiresAt) {
            return;
        }
        if (now - trail.startedAt < trail.windupMs) {
            return;
        }
        if (trail.lastTip == null) {
            trail.lastTip = tip;
            trail.lastTime = now;
            return;
        }
        long elapsed = Math.max(1L, now - trail.lastTime);
        double speed = tip.distanceTo(trail.lastTip) / elapsed;
        trail.lastTip = tip;
        trail.lastTime = now;
        if (!trail.started) {
            if (speed < START_SPEED) {
                trail.pending = new Sample(now, base, tip);
                return;
            }
            trail.started = true;
            trail.samples.clear();
            if (trail.pending != null && now - trail.pending.time() <= PENDING_MS) {
                trail.samples.addLast(trail.pending);
            }
            trail.pending = null;
        }
        trail.samples.addLast(new Sample(now, base, tip));
        if (!trail.finished && trail.endDetector.update(tip, now)
                && now - trail.startedAt >= trail.minFinishMs) {
            trail.finished = true;
            trail.fadeMillis = FINISH_FADE_MS;
        }
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
            while (!trail.samples.isEmpty() && now - trail.samples.peekFirst().time() > trail.fadeMillis) {
                trail.samples.removeFirst();
            }
        }
    }

    public record Sample(long time, Vec3 base, Vec3 tip) {
    }

    static final class Trail {
        final Deque<Sample> samples = new ArrayDeque<>();
        final TrailEndDetector endDetector = new TrailEndDetector();
        long expiresAt;
        long startedAt;
        long windupMs;
        long minFinishMs;
        long fadeMillis = FADE_MS;
        int tint = 0xFFFFFF;
        boolean offhand;
        boolean finished;
        boolean started;
        Vec3 lastTip;
        long lastTime;
        Sample pending;
    }
}
