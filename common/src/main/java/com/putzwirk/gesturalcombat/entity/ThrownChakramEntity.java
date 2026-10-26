package com.putzwirk.gesturalcombat.entity;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.OutLock;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.HitStun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ThrownChakramEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownChakramEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_SAW =
            SynchedEntityData.defineId(ThrownChakramEntity.class, EntityDataSerializers.BOOLEAN);

    private final Map<UUID, Integer> hitCooldowns = new HashMap<>();
    private UUID ownerId;
    private String techniqueId = "";
    private double damage;
    private int age;
    private int outboundTicks = 10;
    private int lifetime = 34;
    private boolean returning;
    private boolean offhand;
    private boolean chain;
    private int sawHits;
    private int hopIndex;
    private int stuckUntil;
    private int pauseUntil;
    private double smoothedX;
    private double smoothedY;
    private double smoothedZ;
    private boolean smoothing;
    private final List<UUID> hops = new ArrayList<>();

    public ThrownChakramEntity(EntityType<? extends ThrownChakramEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static void launch(ServerLevel level, ServerPlayer player, ItemStack stack, Technique technique,
                              double damage, double reach, boolean offhand) {
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
        chakram.offhand = offhand;
        Vec3 direction = player.getLookAngle();
        chakram.setDeltaMovement(direction.normalize().scale(0.7));
        level.addFreshEntity(chakram);
        OutLock.lock(player, offhand);
    }

    public static boolean launchChain(ServerLevel level, ServerPlayer player, ItemStack stack, Technique technique,
                                      double damage, boolean offhand) {
        List<UUID> path = chainPath(level, player);
        if (path.isEmpty()) {
            return false;
        }
        ThrownChakramEntity chakram = base(level, player, stack, technique, damage, offhand);
        chakram.chain = true;
        chakram.hops.addAll(path);
        chakram.outboundTicks = 20 * path.size();
        chakram.lifetime = 20 * path.size() + 40;
        chakram.setDeltaMovement(flatLook(player).scale(0.55));
        level.addFreshEntity(chakram);
        OutLock.lock(player, offhand);
        return true;
    }

    public static boolean launchSaw(ServerLevel level, ServerPlayer player, ItemStack stack, Technique technique,
                                   double damage, boolean offhand) {
        ThrownChakramEntity thrown = base(level, player, stack, technique, damage, offhand);
        thrown.setSaw(true);
        thrown.outboundTicks = 30;
        thrown.lifetime = 96;
        Vec3 flat = flatLook(player);
        thrown.setXRot(0.0f);
        thrown.setPos(thrown.getX() + flat.x * 0.35, thrown.getY(), thrown.getZ() + flat.z * 0.35);
        thrown.setDeltaMovement(flat.scale(1.0));
        level.addFreshEntity(thrown);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS, 0.8f, 0.72f);
        OutLock.lock(player, offhand);
        return true;
    }

    private static ThrownChakramEntity base(ServerLevel level, ServerPlayer player, ItemStack stack,
                                            Technique technique, double damage, boolean offhand) {
        ThrownChakramEntity thrown = new ThrownChakramEntity(ModEntities.THROWN_CHAKRAM.get(), level);
        thrown.setPos(player.getX(), player.getEyeY() - 0.25, player.getZ());
        thrown.setYRot(player.getYRot());
        thrown.setXRot(player.getXRot());
        thrown.setItem(stack);
        thrown.ownerId = player.getUUID();
        thrown.techniqueId = technique.id();
        thrown.damage = Math.max(1.0, damage);
        thrown.offhand = offhand;
        return thrown;
    }

    private static final int SAW_PAUSE = 10;
    private static final double HOP_RADIUS = 8.0;
    private static final double PLAYER_RADIUS = 18.0;
    private static final double AIM_RANGE = 18.0;
    private static final double AIM_SPREAD = 3.5;

    private static List<UUID> chainPath(ServerLevel level, ServerPlayer player) {
        List<UUID> path = new ArrayList<>();
        List<LivingEntity> chosen = new ArrayList<>();
        LivingEntity first = aimTarget(level, player, chosen);
        if (first == null) {
            first = nearest(level, player, player.position(), chosen);
        }
        Vec3 anchor = player.position();
        for (int i = 0; i < 4; i++) {
            LivingEntity next = i == 0 ? first : nearest(level, player, anchor, chosen);
            if (next == null) {
                break;
            }
            chosen.add(next);
            path.add(next.getUUID());
            anchor = next.position();
        }
        return path;
    }

    private static LivingEntity aimTarget(ServerLevel level, ServerPlayer player, List<LivingEntity> chosen) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(AIM_RANGE),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator() && !chosen.contains(entity))) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along < 0.2 || along > AIM_RANGE) {
                continue;
            }
            Vec3 perpendicular = to.subtract(look.scale(along));
            double spread = perpendicular.length() - candidate.getBbWidth() * 0.5;
            if (spread > AIM_SPREAD) {
                continue;
            }
            double score = Math.max(0.0, spread) + along * 0.03;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static LivingEntity nearest(ServerLevel level, ServerPlayer player, Vec3 anchor, List<LivingEntity> chosen) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(anchor, anchor).inflate(HOP_RADIUS),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator() && !chosen.contains(entity))) {
            if (candidate.distanceToSqr(player) > PLAYER_RADIUS * PLAYER_RADIUS) {
                continue;
            }
            double distance = candidate.distanceToSqr(anchor);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public void remove(RemovalReason reason) {
        ServerPlayer owner = owner();
        if (owner != null) {
            OutLock.unlock(owner);
        }
        super.remove(reason);
    }

    public ItemStack getItem() {
        return entityData.get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        entityData.set(DATA_ITEM, stack.copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_SAW, false);
    }

    public boolean isSaw() {
        return entityData.get(DATA_SAW);
    }

    public void setSaw(boolean value) {
        entityData.set(DATA_SAW, value);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            applyPacketSmoothing();
            return;
        }
        age++;
        if (age > lifetime) {
            discard();
            return;
        }
        ServerPlayer owner = owner();
        boolean paused = isSaw() && !returning && age < pauseUntil;
        if (!paused) {
            if (chain && !returning && owner != null) {
                if (age < stuckUntil) {
                    setDeltaMovement(0.0, 0.0, 0.0);
                } else if (!advanceHop(owner)) {
                    returning = true;
                }
            }
            if (!chain && !returning && age >= outboundTicks && age >= pauseUntil) {
                returning = true;
            }
            if (returning) {
                if (owner == null || !owner.isAlive()) {
                    discard();
                    return;
                }
                Vec3 toOwner = owner.getEyePosition().subtract(position());
                if (toOwner.length() < 1.1) {
                    OutLock.unlock(owner);
                    discard();
                    return;
                }
                Vec3 desired = toOwner.normalize().scale(0.8);
                setDeltaMovement(getDeltaMovement().scale(0.55).add(desired.scale(0.55)));
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.985));
            }
            move(MoverType.SELF, getDeltaMovement());
        }
        if (!chain) {
            sweep(owner);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        smoothedX = getX();
        smoothedY = getY();
        smoothedZ = getZ();
        smoothing = true;
        setPos(x, y, z);
        setRot(yRot, xRot);
    }

    private void applyPacketSmoothing() {
        if (!smoothing) {
            return;
        }
        smoothing = false;
        xOld = smoothedX;
        yOld = smoothedY;
        zOld = smoothedZ;
        xo = smoothedX;
        yo = smoothedY;
        zo = smoothedZ;
    }

    private Vec3 heading() {
        Vec3 motion = getDeltaMovement();
        Vec3 flat = new Vec3(motion.x, 0.0, motion.z);
        if (flat.lengthSqr() > 1.0e-6) {
            return flat.normalize();
        }
        Vec3 look = Vec3.directionFromRotation(0.0f, getYRot());
        return new Vec3(look.x, 0.0, look.z).normalize();
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0e-4) {
            return player.getLookAngle();
        }
        return flat.normalize();
    }

    private boolean advanceHop(ServerPlayer owner) {
        while (hopIndex < hops.size()) {
            Entity entity = level() instanceof ServerLevel server ? server.getEntity(hops.get(hopIndex)) : null;
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                hopIndex++;
                continue;
            }
            Vec3 to = target.getBoundingBox().getCenter().subtract(position());
            if (to.length() < 1.4) {
                Technique technique = Techniques.get(techniqueId);
                if (technique != null) {
                    double falloff = Math.max(0.55, 1.0 - 0.15 * hopIndex);
                    EffectSupport.hit(owner, target, (float) (damage * falloff), technique, 0.0, 0.0);
                    HitStun.pin(target, 4);
                    Vec3 direction = flatLook(owner);
                    GestureScheduler.schedule(level(), 4, () -> {
                        if (target.isAlive() && !target.isRemoved()) {
                            target.push(direction.x * 0.35, 0.1, direction.z * 0.35);
                            target.hurtMarked = true;
                        }
                    });
                }
                stuckUntil = age + 6;
                hopIndex++;
                continue;
            }
            setDeltaMovement(to.normalize().scale(0.55));
            return true;
        }
        return false;
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
            if (isSaw()) {
                if (sawHits >= 10) {
                    returning = true;
                    break;
                }
                sawHits++;
                hitCooldowns.put(target.getUUID(), age + SAW_PAUSE + 4);
                pauseUntil = age + SAW_PAUSE;
                EffectSupport.hit(owner, target, (float) (damage * 0.3), technique, 0.0, 0.0);
                Vec3 push = heading().scale(1.45);
                target.push(push.x, 0.05, push.z);
                target.hurtMarked = true;
                level().playSound(null, target.getX(), target.getY() + 0.4, target.getZ(),
                        SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.85f, 1.45f);
                level().playSound(null, target.getX(), target.getY() + 0.4, target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.75f);
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
        setNoGravity(true);
        if (tag.contains("Item")) {
            setItem(ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Item")));
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
        offhand = tag.getBoolean("Offhand");
        setSaw(tag.getBoolean("Saw"));
        sawHits = tag.getInt("SawHits");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", getItem().save(level().registryAccess()));
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putString("Technique", techniqueId);
        tag.putDouble("Damage", damage);
        tag.putInt("Age", age);
        tag.putInt("Outbound", outboundTicks);
        tag.putInt("Lifetime", lifetime);
        tag.putBoolean("Returning", returning);
        tag.putBoolean("Offhand", offhand);
        tag.putBoolean("Saw", isSaw());
        tag.putInt("SawHits", sawHits);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
}
