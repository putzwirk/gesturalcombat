package com.putzwirk.gesturalcombat.server;

import com.putzwirk.gesturalcombat.world.FractureBlockEntity;
import com.putzwirk.gesturalcombat.world.ModTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class TerrainFracture {
    private static final int MIN_LIFE = 26;
    private static final int LIFE_SPREAD = 34;

    private TerrainFracture() {
    }

    public static void spread(ServerLevel level, Vec3 center, double radius, double maxLift) {
        int r = Mth.ceil(radius);
        BlockPos origin = BlockPos.containing(center.x, center.y - 0.2, center.z);
        int placed = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance > radius || distance < 0.6) {
                    continue;
                }
                double t = Mth.clamp(distance / Math.max(0.6, radius), 0.0, 1.0);
                if (level.random.nextFloat() > 0.55 + 0.35 * t) {
                    continue;
                }
                BlockPos ground = surface(level, origin.offset(dx, 1, dz), origin.getY() + 2, origin.getY() - 3);
                if (ground == null) {
                    continue;
                }
                BlockPos target = ground.above();
                boolean refresh = level.getBlockState(target).is(ModTerrain.FRACTURE.get());
                if (!refresh && !level.getBlockState(target).canBeReplaced()) {
                    continue;
                }
                BlockState state = level.getBlockState(ground);
                if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
                    continue;
                }
                level.setBlock(target, ModTerrain.FRACTURE.get().defaultBlockState(), 2);
                if (level.getBlockEntity(target) instanceof FractureBlockEntity fracture) {
                    float lift = (float) (maxLift * (0.35 + 0.65 * t));
                    float yaw = level.random.nextFloat() * 360.0F;
                    float tiltX = (level.random.nextFloat() - 0.5F) * 9.0F;
                    float tiltZ = (level.random.nextFloat() - 0.5F) * 9.0F;
                    int life = MIN_LIFE + level.random.nextInt(LIFE_SPREAD);
                    fracture.configure(state, lift, yaw, tiltX, tiltZ, life);
                    level.sendBlockUpdated(target, fracture.getBlockState(), fracture.getBlockState(), 2);
                }
                if (state.getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            target.getX() + 0.5, target.getY() + 0.15, target.getZ() + 0.5, 2,
                            0.28, 0.12, 0.28, 0.10);
                }
                placed++;
                if (placed > 70) {
                    return;
                }
            }
        }
    }

    private static BlockPos surface(ServerLevel level, BlockPos from, int top, int bottom) {
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(from.getX(), y, from.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.is(ModTerrain.FRACTURE.get())) {
                continue;
            }
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            return pos;
        }
        return null;
    }
}
