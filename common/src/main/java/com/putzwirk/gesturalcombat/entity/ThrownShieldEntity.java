package com.putzwirk.gesturalcombat.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ThrownShieldEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownShieldEntity.class, EntityDataSerializers.ITEM_STACK);
    private int life;

    public ThrownShieldEntity(EntityType<? extends ThrownShieldEntity> type, Level level) {
        super(type, level);
    }

    public ThrownShieldEntity(EntityType<? extends ThrownShieldEntity> type, Level level, Vec3 pos, Vec3 velocity, ItemStack stack) {
        this(type, level);
        setPos(pos.x, pos.y, pos.z);
        setDeltaMovement(velocity);
        setItem(stack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    public ItemStack getItem() {
        return entityData.get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        entityData.set(DATA_ITEM, stack.copy());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            Vec3 motion = getDeltaMovement();
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
            setDeltaMovement(motion.scale(0.98));
            if (++life > 40) {
                discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Item")) {
            setItem(ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Item")));
        }
        life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", getItem().save(level().registryAccess()));
        tag.putInt("Life", life);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
