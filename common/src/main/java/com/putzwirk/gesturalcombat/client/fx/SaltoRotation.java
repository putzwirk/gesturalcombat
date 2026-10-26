package com.putzwirk.gesturalcombat.client.fx;

import net.minecraft.Util;

import java.util.HashMap;
import java.util.Map;

public final class SaltoRotation {
    private static final double HIP_HEIGHT = 0.75;
    private static final double START = 0.05;
    private static final double END = 0.95;
    private static final Map<Integer, Entry> ACTIVE = new HashMap<>();

    private record Entry(long startMillis, long durationMillis) {
    }

    private SaltoRotation() {
    }

    public static void begin(int entityId, int ticks) {
        long now = Util.getMillis();
        if (ACTIVE.size() > 32) {
            ACTIVE.entrySet().removeIf(entry -> now - entry.getValue().startMillis() > entry.getValue().durationMillis());
        }
        ACTIVE.put(entityId, new Entry(now, Math.max(120L, ticks * 50L)));
    }

    public static void clear(int entityId) {
        ACTIVE.remove(entityId);
    }

    public static float hips() {
        return (float) HIP_HEIGHT;
    }

    public static float angle(int entityId) {
        Entry entry = ACTIVE.get(entityId);
        if (entry == null) {
            return 0.0f;
        }
        double progress = (Util.getMillis() - entry.startMillis()) / (double) entry.durationMillis();
        if (progress >= 1.0) {
            ACTIVE.remove(entityId);
            return 0.0f;
        }
        double window = (progress - START) / (END - START);
        if (window <= 0.0) {
            return 0.0f;
        }
        if (window >= 1.0) {
            return 360.0f;
        }
        double eased = window * window * (3.0 - 2.0 * window);
        return (float) (eased * 360.0);
    }
}
