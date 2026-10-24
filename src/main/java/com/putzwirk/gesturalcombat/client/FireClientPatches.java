package com.putzwirk.gesturalcombat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FireClientPatches {
    private static final List<Patch> PATCHES = new ArrayList<>();

    private FireClientPatches() {
    }

    public static void add(double x, double y, double z, double radius, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        PATCHES.add(new Patch(x, y, z, radius, mc.level.getGameTime() + Math.max(20, durationTicks)));
    }

    public static boolean protects(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        long now = mc.level.getGameTime();
        Iterator<Patch> iterator = PATCHES.iterator();
        boolean inside = false;
        while (iterator.hasNext()) {
            Patch patch = iterator.next();
            if (now > patch.until) {
                iterator.remove();
                continue;
            }
            double dx = pos.getX() + 0.5 - patch.x;
            double dz = pos.getZ() + 0.5 - patch.z;
            double limit = patch.radius + 2.0;
            if (dx * dx + dz * dz <= limit * limit && Math.abs(pos.getY() - patch.y) <= 3.0) {
                inside = true;
            }
        }
        return inside;
    }

    public static void clear() {
        PATCHES.clear();
    }

    private record Patch(double x, double y, double z, double radius, long until) {
    }
}
