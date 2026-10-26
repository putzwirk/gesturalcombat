package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class FirePatchManager {
    public static final String TAG = "gesturalcombat_fire_arrow";

    private static final int GROWTH_TICKS = 8;
    private static final int MAX_BLOCKS = 24;
    private static final int REMOVE_PER_TICK = 2;

    private static final List<Patch> PATCHES = new ArrayList<>();

    private FirePatchManager() {
    }

    public static void spawn(ServerLevel level, Vec3 pos, int durationTicks, double radius) {
        double clamped = Math.max(1.0, radius);
        PATCHES.add(new Patch(level, BlockPos.containing(pos), level.getGameTime() + Math.max(20, durationTicks), clamped));
        com.putzwirk.gesturalcombat.network.ModNetwork.sendNear(level, pos.x, pos.y, pos.z, 96.0,
                new com.putzwirk.gesturalcombat.network.FirePatchPacket(pos.x, pos.y, pos.z, clamped, durationTicks));
    }

    public static boolean protects(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return com.putzwirk.gesturalcombat.client.FireClientPatches.protects(pos);
        }
        if (PATCHES.isEmpty()) {
            return false;
        }
        for (Patch patch : PATCHES) {
            if (patch.level == level && patch.blocks.contains(pos)) {
                return true;
            }
        }
        return false;
    }

        public static void tick() {
        if (PATCHES.isEmpty()) {
            return;
        }
        Iterator<Patch> iterator = PATCHES.iterator();
        while (iterator.hasNext()) {
            Patch patch = iterator.next();
            if (patch.level.getServer() == null) {
                iterator.remove();
                continue;
            }
            if (!patch.update(patch.level.getGameTime())) {
                iterator.remove();
            }
        }
    }

        public static void stopped() {
        PATCHES.clear();
    }

    private static final class Patch {
        private final ServerLevel level;
        private final BlockPos center;
        private final long until;
        private final double radius;
        private final Set<BlockPos> blocks = new HashSet<>();

        private Patch(ServerLevel level, BlockPos center, long until, double radius) {
            this.level = level;
            this.center = center;
            this.until = until;
            this.radius = radius;
            placeIfValid(center);
            placeIfValid(center.above());
            placeIfValid(center.east());
            placeIfValid(center.west());
            placeIfValid(center.north());
            placeIfValid(center.south());
        }

        private boolean update(long now) {
            if (now < until) {
                grow();
                keepAlive();
                return true;
            }
            return removeStep();
        }

        private void grow() {
            if (blocks.size() >= MAX_BLOCKS) {
                return;
            }
            if (level.getGameTime() % 2 != 0) {
                return;
            }
            List<BlockPos> existing = new ArrayList<>(blocks);
            for (BlockPos origin : existing) {
                for (int attempt = 0; attempt < 2; attempt++) {
                    int dx = level.random.nextInt(3) - 1;
                    int dz = level.random.nextInt(3) - 1;
                    int dy = level.random.nextInt(5) == 0 ? (level.random.nextBoolean() ? 1 : -1) : 0;
                    placeIfValid(origin.offset(dx, dy, dz));
                    if (blocks.size() >= MAX_BLOCKS) {
                        return;
                    }
                }
            }
            if (existing.size() < 4) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                double distance = 1.0 + level.random.nextDouble() * radius;
                BlockPos spread = BlockPos.containing(center.getX() + 0.5 + Math.cos(angle) * distance,
                        center.getY() + 0.5, center.getZ() + 0.5 + Math.sin(angle) * distance);
                placeIfValid(findSupport(spread));
            }
        }

        private void keepAlive() {
            for (BlockPos pos : blocks) {
                if (!level.getBlockState(pos).is(Blocks.FIRE)) {
                    level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 2);
                }
            }
        }

        private boolean removeStep() {
            int removed = 0;
            Iterator<BlockPos> iterator = blocks.iterator();
            while (iterator.hasNext() && removed < REMOVE_PER_TICK) {
                BlockPos pos = iterator.next();
                if (level.getBlockState(pos).is(Blocks.FIRE)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                iterator.remove();
                removed++;
            }
            return blocks.isEmpty();
        }

        private void placeIfValid(BlockPos pos) {
            if (blocks.size() >= MAX_BLOCKS || blocks.contains(pos)) {
                return;
            }
            if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()) {
                return;
            }
            if (Math.abs(pos.getX() - center.getX()) > radius + 2
                    || Math.abs(pos.getZ() - center.getZ()) > radius + 2
                    || Math.abs(pos.getY() - center.getY()) > 3) {
                return;
            }
            BlockState fire = Blocks.FIRE.defaultBlockState();
            if (!fire.canSurvive(level, pos)) {
                return;
            }
            level.setBlock(pos, fire, 2);
            blocks.add(pos.immutable());
        }

        private BlockPos findSupport(BlockPos pos) {
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos candidate = pos.offset(0, dy, 0);
                BlockState fire = Blocks.FIRE.defaultBlockState();
                if (level.getBlockState(candidate).isAir() && fire.canSurvive(level, candidate)) {
                    return candidate;
                }
            }
            return pos;
        }
    }
}
