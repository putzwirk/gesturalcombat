package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.mastery.PlayerMastery;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ClientState {
    public static final PlayerMastery MASTERY = new PlayerMastery();
    public static final List<TrailPoint> TRAIL = new ArrayList<>();
    public static final Map<String, Integer> COOLDOWN_END = new HashMap<>();
    private static final Map<String, Integer> COOLDOWN_TOTAL = new HashMap<>();
    private static int activeTrailId;
    private static Archetype activeTrailArchetype;

    public static boolean synced;
    public static int ticks;
    public static boolean stroking;
    public static int strokeTicks;
    public static int strokeStrength;
    public static GestureShape shownShape = GestureShape.UNKNOWN;
    public static float shownConfidence;
    public static String lastTechnique = "";
    public static int flashTicks;
    public static int castTicks;
    public static int rejectTicks;
    public static int guardTicks;
    public static int shieldLockTicks;
    public static String pendingTechnique = "";
    public static int pendingTicks;

    private ClientState() {
    }

    public record TrailPoint(float x, float y, int tick, int trailId, Archetype archetype) {
    }

    public static void applyMastery(CompoundTag tag) {
        MASTERY.load(tag);
        synced = true;
    }

    public static void reset() {
        MASTERY.load(new CompoundTag());
        synced = false;
        TRAIL.clear();
        COOLDOWN_END.clear();
        COOLDOWN_TOTAL.clear();
        stroking = false;
        strokeTicks = 0;
        strokeStrength = 0;
        shownShape = GestureShape.UNKNOWN;
        shownConfidence = 0.0f;
        lastTechnique = "";
        flashTicks = 0;
        castTicks = 0;
        rejectTicks = 0;
        guardTicks = 0;
        shieldLockTicks = 0;
        pendingTechnique = "";
        pendingTicks = 0;
    }

    @Nullable
    public static Archetype currentArchetype() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        Archetype main = Archetype.of(mc.player.getMainHandItem());
        if (main != null) {
            return main;
        }
        return Archetype.of(mc.player.getOffhandItem());
    }

    public static void beginTrail(Archetype archetype) {
        activeTrailId++;
        activeTrailArchetype = archetype;
    }

    public static void pushTrailPoint(float x, float y) {
        TRAIL.add(new TrailPoint(x, y, ticks, activeTrailId, activeTrailArchetype));
        while (TRAIL.size() > 2048) {
            TRAIL.remove(0);
        }
    }

    public static void pruneTrail(int maxAge) {
        Iterator<TrailPoint> iterator = TRAIL.iterator();
        while (iterator.hasNext()) {
            if (ticks - iterator.next().tick() > maxAge) {
                iterator.remove();
            }
        }
    }

    public static int cooldownRemaining(String techniqueId) {
        int end = COOLDOWN_END.getOrDefault(techniqueId, 0);
        return Math.max(0, end - ticks);
    }

    public static float cooldownFraction(String techniqueId, int totalTicks) {
        int total = COOLDOWN_TOTAL.getOrDefault(techniqueId, totalTicks);
        if (total <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, cooldownRemaining(techniqueId) / (float) total);
    }

    public static int cooldownTotal(String key, int fallback) {
        return COOLDOWN_TOTAL.getOrDefault(key, fallback);
    }

    public static void startCooldown(String key, int ticksValue) {
        COOLDOWN_END.put(key, ticks + ticksValue);
        COOLDOWN_TOTAL.put(key, Math.max(1, ticksValue));
    }

    public static boolean isReady(String techniqueId) {
        return cooldownRemaining(techniqueId) <= 0;
    }
}
