package com.putzwirk.gesturalcombat.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FractureBlockEntity extends BlockEntity {
    private BlockState original = Blocks.STONE.defaultBlockState();
    private float lift;
    private float yaw;
    private float tiltX;
    private float tiltZ;
    private int life = 40;
    private int age;

    public FractureBlockEntity(BlockPos pos, BlockState state) {
        super(ModTerrain.FRACTURE_ENTITY.get(), pos, state);
    }

    public void configure(BlockState original, float lift, float yaw, float tiltX, float tiltZ, int life) {
        this.original = original;
        this.lift = lift;
        this.yaw = yaw;
        this.tiltX = tiltX;
        this.tiltZ = tiltZ;
        this.life = Math.max(10, life);
        this.age = 0;
        this.setChanged();
    }

    public BlockState original() {
        return original;
    }

    public float lift() {
        return lift;
    }

    public float yaw() {
        return yaw;
    }

    public float tiltX() {
        return tiltX;
    }

    public float tiltZ() {
        return tiltZ;
    }

    public float age() {
        return age;
    }

    public float life() {
        return life;
    }

    public void tick(Level level, BlockPos pos) {
        age++;
        if (!level.isClientSide() && age >= life) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Original", net.minecraft.nbt.NbtUtils.writeBlockState(original));
        tag.putFloat("Lift", lift);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("TiltX", tiltX);
        tag.putFloat("TiltZ", tiltZ);
        tag.putInt("Life", life);
        tag.putInt("Age", age);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Original")) {
            original = net.minecraft.nbt.NbtUtils.readBlockState(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Original"));
        }
        lift = tag.getFloat("Lift");
        yaw = tag.getFloat("Yaw");
        tiltX = tag.getFloat("TiltX");
        tiltZ = tag.getFloat("TiltZ");
        life = Math.max(10, tag.getInt("Life"));
        age = tag.getInt("Age");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
