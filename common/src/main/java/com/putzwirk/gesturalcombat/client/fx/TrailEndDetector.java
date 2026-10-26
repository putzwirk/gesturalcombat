package com.putzwirk.gesturalcombat.client.fx;

import net.minecraft.world.phys.Vec3;

public final class TrailEndDetector {
    private static final int WARMUP_SAMPLES = 6;
    private static final long WARMUP_MS = 150L;
    private static final long QUIET_MS = 110L;
    private static final double QUIET_RATIO = 0.18;
    private static final double MIN_PEAK_SPEED = 0.0012;
    private static final double MIN_TRAVEL = 0.35;

    private Vec3 firstTip;
    private Vec3 lastTip;
    private long firstTime;
    private long lastTime;
    private double peakSpeed;
    private double smoothSpeed;
    private long quietSince;
    private int samples;
    private boolean finished;

    public boolean update(Vec3 tip, long now) {
        if (finished) {
            return true;
        }
        samples++;
        if (firstTip == null) {
            firstTip = tip;
            lastTip = tip;
            firstTime = now;
            lastTime = now;
            return false;
        }
        long elapsed = Math.max(1L, now - lastTime);
        lastTime = now;
        double speed = tip.distanceTo(lastTip) / elapsed;
        lastTip = tip;
        smoothSpeed = smoothSpeed == 0.0 ? speed : smoothSpeed * 0.65 + speed * 0.35;
        if (smoothSpeed >= peakSpeed) {
            peakSpeed = smoothSpeed;
            quietSince = 0L;
        } else if (smoothSpeed < peakSpeed * QUIET_RATIO) {
            if (quietSince == 0L) {
                quietSince = now;
            }
        } else {
            quietSince = 0L;
        }
        if (quietSince != 0L
                && now - quietSince >= QUIET_MS
                && samples >= WARMUP_SAMPLES
                && now - firstTime >= WARMUP_MS
                && peakSpeed >= MIN_PEAK_SPEED
                && tip.distanceTo(firstTip) >= MIN_TRAVEL) {
            finished = true;
        }
        return finished;
    }
}
