package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.core.GestureShape;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class States {
    private static final int SEAM_CAP = 3;
    private static final int SEAM_TICKS = 60;
    private static final int SUNDER_CAP = 3;
    private static final int SUNDER_TICKS = 100;
    private static final int BLEED_CAP = 5;
    private static final int BLEED_TICKS = 60;
    private static final int BLEED_PULSE = 20;
    private static final int DEBT_CAP = 10;
    private static final int FLOW_CAP = 3;
    private static final int FLOW_DECAY = 30;
    private static final int HARVEST_TICKS = 40;
    private static final int HARVEST_TARGET = 5;
    private static final int DOMAIN_CAP = 12;
    private static final int DUAL_TICKS = 8;

    private static final Map<UUID, Long> STAGGER = new HashMap<>();
    private static final Map<UUID, Timed> SEAM = new HashMap<>();
    private static final Map<UUID, Timed> SUNDER = new HashMap<>();
    private static final Map<UUID, Bleed> BLEED = new HashMap<>();
    private static final Map<UUID, Integer> DEBT = new HashMap<>();
    private static final Map<UUID, Flow> FLOW = new HashMap<>();
    private static final Map<UUID, Edge> EDGE = new HashMap<>();
    private static final Map<UUID, Map<UUID, Long>> HARVEST = new HashMap<>();
    private static final Map<UUID, Integer> DOMAIN = new HashMap<>();
    private static final Map<UUID, long[]> DUAL = new HashMap<>();
    private static final Map<UUID, long[]> PAIR = new HashMap<>();
    private static final int PAIR_TTL = 200;

    private States() {
    }

    public static void stagger(LivingEntity target, int ticks) {
        if (target.level().isClientSide()) {
            return;
        }
        STAGGER.put(target.getUUID(), target.level().getGameTime() + Math.max(1, ticks));
    }

    public static boolean staggered(LivingEntity target) {
        Long until = STAGGER.get(target.getUUID());
        return until != null && target.level().getGameTime() < until;
    }

    public static int seam(LivingEntity target) {
        Timed timed = SEAM.get(target.getUUID());
        return timed == null ? 0 : timed.value;
    }

    public static int addSeam(LivingEntity target, int amount) {
        return add(SEAM, target, amount, SEAM_CAP, SEAM_TICKS);
    }

    public static int consumeSeam(LivingEntity target) {
        Timed timed = SEAM.remove(target.getUUID());
        return timed == null ? 0 : timed.value;
    }

    public static int sunder(LivingEntity target) {
        Timed timed = SUNDER.get(target.getUUID());
        return timed == null ? 0 : timed.value;
    }

    public static int addSunder(LivingEntity target, int amount) {
        return add(SUNDER, target, amount, SUNDER_CAP, SUNDER_TICKS);
    }

    public static int bleed(LivingEntity target) {
        Bleed bleed = BLEED.get(target.getUUID());
        return bleed == null ? 0 : bleed.stacks;
    }

    public static int addBleed(LivingEntity target, ServerPlayer owner, int amount) {
        if (target.level().isClientSide() || amount <= 0) {
            return 0;
        }
        long now = target.level().getGameTime();
        Bleed bleed = BLEED.computeIfAbsent(target.getUUID(), id -> new Bleed(owner.getUUID(), now));
        bleed.stacks = Math.min(BLEED_CAP, bleed.stacks + amount);
        bleed.expiry = now + BLEED_TICKS;
        bleed.owner = owner.getUUID();
        return bleed.stacks;
    }

    public static void refreshBleed(LivingEntity target, ServerPlayer owner, int extra) {
        Bleed bleed = BLEED.get(target.getUUID());
        if (bleed == null) {
            addBleed(target, owner, extra);
            return;
        }
        long now = target.level().getGameTime();
        bleed.expiry = now + BLEED_TICKS;
        if (extra > 0) {
            bleed.stacks = Math.min(BLEED_CAP, bleed.stacks + extra);
        }
    }

    public static int consumeBleed(LivingEntity target) {
        Bleed bleed = BLEED.remove(target.getUUID());
        return bleed == null ? 0 : bleed.stacks;
    }

    public static int debt(ServerPlayer player) {
        return DEBT.getOrDefault(player.getUUID(), 0);
    }

    public static int addDebt(ServerPlayer player, int amount) {
        int value = Math.min(DEBT_CAP, debt(player) + Math.max(0, amount));
        DEBT.put(player.getUUID(), value);
        return value;
    }

    public static int spendDebt(ServerPlayer player) {
        Integer value = DEBT.remove(player.getUUID());
        return value == null ? 0 : value;
    }

    public static int flow(ServerPlayer player) {
        Flow flow = FLOW.get(player.getUUID());
        if (flow == null) {
            return 0;
        }
        long now = player.level().getGameTime();
        if (now - flow.lastTick > FLOW_DECAY) {
            flow.stacks = Math.max(0, flow.stacks - (int) ((now - flow.lastTick) / FLOW_DECAY));
            flow.lastTick = now;
        }
        return flow.stacks;
    }

    public static int addFlow(ServerPlayer player, GestureShape shape) {
        Flow flow = FLOW.computeIfAbsent(player.getUUID(), id -> new Flow());
        long now = player.level().getGameTime();
        if (flow.lastShape != null && flow.lastShape != shape) {
            flow.stacks = Math.min(FLOW_CAP, flow.stacks + 1);
        }
        flow.lastShape = shape;
        flow.lastTick = now;
        return flow.stacks;
    }

    public static boolean alternateEdge(ServerPlayer player) {
        Edge edge = EDGE.computeIfAbsent(player.getUUID(), id -> new Edge());
        long now = player.level().getGameTime();
        boolean continuous = edge.last >= 0 && now - edge.tick <= 200;
        edge.last = continuous ? 1 - edge.last : 0;
        edge.tick = now;
        return continuous;
    }

    public static int harvest(ServerPlayer player) {
        Map<UUID, Long> seen = HARVEST.get(player.getUUID());
        if (seen == null) {
            return 0;
        }
        long now = player.level().getGameTime();
        seen.entrySet().removeIf(entry -> entry.getValue() < now);
        return seen.size();
    }

    public static int recordHarvest(ServerPlayer player, LivingEntity target) {
        long now = player.level().getGameTime();
        Map<UUID, Long> seen = HARVEST.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        seen.put(target.getUUID(), now + HARVEST_TICKS);
        seen.entrySet().removeIf(entry -> entry.getValue() < now);
        return seen.size();
    }

    public static boolean harvestBonus(ServerPlayer player) {
        return harvest(player) >= HARVEST_TARGET;
    }

    public static int harvestTarget() {
        return HARVEST_TARGET;
    }

    public static int domain(ServerPlayer player) {
        return DOMAIN.getOrDefault(player.getUUID(), 0);
    }

    public static void addDomain(ServerPlayer player, int amount) {
        DOMAIN.put(player.getUUID(), Math.min(DOMAIN_CAP, domain(player) + Math.max(0, amount)));
    }

    public static int spendDomain(ServerPlayer player, int amount) {
        int value = domain(player);
        int spend = Math.min(value, Math.max(0, amount));
        DOMAIN.put(player.getUUID(), value - spend);
        return spend;
    }

    public static boolean linkedCast(ServerPlayer player, boolean offhand, long now) {
        int hand = offhand ? 1 : 0;
        long[] previous = DUAL.get(player.getUUID());
        boolean linked = previous != null && previous[1] != hand && now - previous[0] <= DUAL_TICKS;
        DUAL.put(player.getUUID(), new long[]{now, hand});
        return linked;
    }

    public static boolean pairLink(ServerPlayer player, boolean offhand, long now, int window) {
        int hand = offhand ? 1 : 0;
        long[] previous = PAIR.get(player.getUUID());
        boolean linked = previous != null && previous[1] != hand && now - previous[0] <= window;
        PAIR.put(player.getUUID(), new long[]{now, hand});
        return linked;
    }

    public static void clear() {
        STAGGER.clear();
        SEAM.clear();
        SUNDER.clear();
        BLEED.clear();
        DEBT.clear();
        FLOW.clear();
        EDGE.clear();
        HARVEST.clear();
        DOMAIN.clear();
        DUAL.clear();
        PAIR.clear();
    }

    private static int add(Map<UUID, Timed> map, LivingEntity target, int amount, int cap, int ticks) {
        if (target.level().isClientSide() || amount <= 0) {
            return 0;
        }
        Timed timed = map.computeIfAbsent(target.getUUID(), id -> new Timed());
        timed.value = Math.min(cap, timed.value + amount);
        timed.expiry = target.level().getGameTime() + ticks;
        return timed.value;
    }

        public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        STAGGER.entrySet().removeIf(entry -> entry.getValue() < now);
        PAIR.entrySet().removeIf(entry -> entry.getValue()[0] + PAIR_TTL < now);
        SEAM.entrySet().removeIf(entry -> entry.getValue().expiry < now);
        SUNDER.entrySet().removeIf(entry -> entry.getValue().expiry < now);
        if (!BLEED.isEmpty()) {
            Iterator<Map.Entry<UUID, Bleed>> iterator = BLEED.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Bleed> entry = iterator.next();
                Bleed bleed = entry.getValue();
                Entity target = findEntity(server, entry.getKey());
                if (!(target instanceof LivingEntity living) || !living.isAlive() || bleed.expiry < now) {
                    iterator.remove();
                    continue;
                }
                if (now < bleed.nextPulse) {
                    continue;
                }
                bleed.nextPulse = now + BLEED_PULSE;
                ServerPlayer owner = server.getPlayerList().getPlayer(bleed.owner);
                if (owner == null) {
                    iterator.remove();
                    continue;
                }
                living.invulnerableTime = 0;
                living.hurt(owner.damageSources().playerAttack(owner), bleed.stacks);
            }
        }
        if (!HARVEST.isEmpty()) {
            for (Map<UUID, Long> seen : HARVEST.values()) {
                seen.entrySet().removeIf(entry -> entry.getValue() < now);
            }
            HARVEST.entrySet().removeIf(entry -> entry.getValue().isEmpty());
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

    private static final class Timed {
        int value;
        long expiry;
    }

    private static final class Bleed {
        UUID owner;
        int stacks;
        long expiry;
        long nextPulse;

        Bleed(UUID owner, long now) {
            this.owner = owner;
            this.expiry = now + BLEED_TICKS;
            this.nextPulse = now + BLEED_PULSE;
        }
    }

    private static final class Flow {
        int stacks;
        GestureShape lastShape;
        long lastTick;
    }

    private static final class Edge {
        int last = -1;
        long tick;
    }
}
