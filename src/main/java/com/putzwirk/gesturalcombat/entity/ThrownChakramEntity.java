package com.putzwirk.gesturalcombat.entity;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThrownChakramEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownChakramEntity.class, EntityDataSerializers.ITEM_STACK);

    private final Map<UUID, Integer> hitCooldowns = new HashMap<>();
    private UUID ownerId;
    private String techniqueId = "";
    private double damage;
    private int age;
    private int outboundTicks = 10;
    private int lifetime = 34;
    private boolean returning;

    public ThrownChakramEntity(EntityType<? extends ThrownChakramEntity> type, Level level) {
        super(type, level);
    }

    public static void launch(ServerLevel level, ServerPlayer player, ItemStack stack, Technique technique,
                              double damage, double reach) {
        ThrownChakramEntity chakram = new ThrownChakramEntity(ModEntities.THROWN_CHAKRAM.get(), level);
        chakram.setPos(player.getX(), player.getEyeY() - 0.25, player.getZ());
        chakram.setYRot(player.getYRot());
        chakram.setXRot(player.getXRot());
        chakram.setItem(stack);
        chakram.ownerId = player.getUUID();
        chakram.techniqueId = technique.id();
        chakram.damage = Math.max(1.0, damage);
        chakram.outboundTicks = (int) Math.max(6, Math.min(26, Math.round(reach / 0.7)));
        chakram.lifetime = chakram.outboundTicks * 3 + 6;
        Vec3 direction = player.getLookAngle();
        chakram.setDeltaMovement(direction.normalize().scale(0.7));
        level.addFreshEntity(chakram);
    }

    public ItemStack getItem() {
        return entityData.get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        entityData.set(DATA_ITEM, stack.copy());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        age++;
        if (age > lifetime) {
            discard();
            return;
        }
        ServerPlayer owner = owner();
        if (age >= outboundTicks) {
            returning = true;
        }
        if (returning) {
            if (owner == null || !owner.isAlive()) {
                discard();
                return;
            }
            Vec3 toOwner = owner.getEyePosition().subtract(position());
            if (toOwner.length() < 1.1) {
                discard();
                return;
            }
            Vec3 desired = toOwner.normalize().scale(0.8);
            setDeltaMovement(getDeltaMovement().scale(0.55).add(desired.scale(0.55)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.985));
        }
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        sweep(owner);
    }

    private void sweep(ServerPlayer owner) {
        if (owner == null) {
            return;
        }
        Technique technique = Techniques.get(techniqueId);
        if (technique == null) {
            return;
        }
        AABB area = getBoundingBox().inflate(0.9);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive() && !entity.isSpectator())) {
            int cooldown = hitCooldowns.getOrDefault(target.getUUID(), 0);
            if (age < cooldown) {
                continue;
            }
            hitCooldowns.put(target.getUUID(), age + 8);
            EffectSupport.hit(owner, target, (float) damage, technique, 0.4, 0.0);
        }
    }

    private ServerPlayer owner() {
        if (ownerId == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        Entity entity = server.getEntity(ownerId);
        return entity instanceof ServerPlayer player ? player : null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Item")) {
            setItem(ItemStack.of(tag.getCompound("Item")));
        }
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }
        techniqueId = tag.getString("Technique");
        damage = tag.getDouble("Damage");
        age = tag.getInt("Age");
        outboundTicks = Math.max(1, tag.getInt("Outbound"));
        lifetime = Math.max(1, tag.getInt("Lifetime"));
        returning = tag.getBoolean("Returning");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", getItem().save(new CompoundTag()));
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putString("Technique", techniqueId);
        tag.putDouble("Damage", damage);
        tag.putInt("Age", age);
        tag.putInt("Outbound", outboundTicks);
        tag.putInt("Lifetime", lifetime);
        tag.putBoolean("Returning", returning);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
}
