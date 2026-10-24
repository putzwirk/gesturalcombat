package com.putzwirk.gesturalcombat.client.fx;

import net.minecraft.world.phys.Vec3;

public final class TrailEndDetector {
    private static final int MIN_SAMPLES = 5;
    private static final long QUIET_MS = 55L;
    private static final double QUIET_RATIO = 0.22;
    private static final double MIN_PEAK_SPEED = 0.0012;

    private Vec3 lastTip;
    private long lastTime;
    private double peakSpeed;
    private long quietSince;
    private int samples;
    private boolean finished;

    public boolean update(Vec3 tip, long now) {
        if (finished) {
            return true;
        }
        samples++;
        if (lastTip == null) {
            lastTip = tip;
            lastTime = now;
            return false;
        }
        long elapsed = Math.max(1L, now - lastTime);
        lastTime = now;
        double speed = tip.distanceTo(lastTip) / elapsed;
        lastTip = tip;
        if (speed >= peakSpeed) {
            peakSpeed = speed;
            quietSince = 0L;
        } else if (peakSpeed >= MIN_PEAK_SPEED && speed < peakSpeed * QUIET_RATIO) {
            if (quietSince == 0L) {
                quietSince = now;
            } else if (now - quietSince >= QUIET_MS) {
                finished = true;
            }
        } else {
            quietSince = 0L;
        }
        return finished;
    }
}
