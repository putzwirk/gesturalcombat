package com.putzwirk.gesturalcombat.entity;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.OutLock;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.HitStun;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private boolean offhand;
    private boolean chain;
    private boolean airtimeScale;
    private boolean saw;
    private int sawHits;
    private int hopIndex;
    private final List<UUID> hops = new ArrayList<>();

    public ThrownChakramEntity(EntityType<? extends ThrownChakramEntity> type, Level level) {
        super(type, level);
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
        chakram.outboundTicks = 8 * path.size();
        chakram.lifetime = 8 * path.size() + 30;
        chakram.setDeltaMovement(player.getLookAngle().normalize().scale(0.9));
        level.addFreshEntity(chakram);
        OutLock.lock(player, offhand);
        return true;
    }

    public static boolean launchWeapon(ServerLevel level, ServerPlayer player, ItemStack stack, Technique technique,
                                       double damage, double reach, boolean offhand, boolean airtimeScale) {
        ThrownChakramEntity thrown = base(level, player, stack, technique, damage, offhand);
        thrown.airtimeScale = airtimeScale;
        thrown.outboundTicks = (int) Math.max(8, Math.min(30, Math.round(reach / 0.55)));
        thrown.lifetime = thrown.outboundTicks * 3 + 10;
        thrown.setDeltaMovement(player.getLookAngle().normalize().scale(airtimeScale ? 0.55 : 0.95));
        level.addFreshEntity(thrown);
        OutLock.lock(player, offhand);
        return true;
    }

    public static boolean launchSaw(ServerLevel level, ServerPlayer player, ItemStack stack, Technique technique,
                                   double damage, boolean offhand) {
        ThrownChakramEntity thrown = base(level, player, stack, technique, damage, offhand);
        thrown.saw = true;
        thrown.outboundTicks = 24;
        thrown.lifetime = 76;
        thrown.setDeltaMovement(player.getLookAngle().normalize().scale(1.0));
        level.addFreshEntity(thrown);
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
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spinTrail();
            return;
        }
        age++;
        if (age > lifetime) {
            discard();
            return;
        }
        ServerPlayer owner = owner();
        if (chain && !returning && owner != null) {
            if (!advanceHop(owner)) {
                returning = true;
            }
        }
        if (!chain && age >= outboundTicks) {
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
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (!chain) {
            sweep(owner);
        }
    }

    private void spinTrail() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0e-5) {
            return;
        }
        Vec3 side = motion.normalize().cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0e-5) {
            return;
        }
        side = side.normalize().scale(0.42);
        level().addParticle(ParticleTypes.END_ROD, getX() + side.x, getY(), getZ() + side.z, 0.0, 0.0, 0.0);
        level().addParticle(ParticleTypes.END_ROD, getX() - side.x, getY(), getZ() - side.z, 0.0, 0.0, 0.0);
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
                    EffectSupport.hit(owner, target, (float) (damage * falloff), technique, 0.3, 0.0);
                }
                hopIndex++;
                continue;
            }
            setDeltaMovement(to.normalize().scale(0.95));
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
            if (saw) {
                if (sawHits >= 10) {
                    returning = true;
                    break;
                }
                sawHits++;
                hitCooldowns.put(target.getUUID(), age + 4);
                EffectSupport.hit(owner, target, (float) (damage * 0.3), technique, 0.0, 0.0);
                HitStun.pin(target, 5);
                Vec3 direction = getDeltaMovement();
                if (direction.lengthSqr() > 1.0e-5) {
                    Vec3 push = direction.normalize().scale(0.5);
                    GestureScheduler.schedule(level(), 5, () -> {
                        if (target.isAlive() && !target.isRemoved()) {
                            target.push(push.x, 0.12, push.z);
                            target.hurtMarked = true;
                        }
                    });
                }
                continue;
            }
            hitCooldowns.put(target.getUUID(), age + 8);
            double scaled = airtimeScale ? damage * (1.0 + Math.min(0.6, age * 0.02)) : damage;
            EffectSupport.hit(owner, target, (float) scaled, technique, 0.4, 0.0);
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
        offhand = tag.getBoolean("Offhand");
        airtimeScale = tag.getBoolean("Airtime");
        saw = tag.getBoolean("Saw");
        sawHits = tag.getInt("SawHits");
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
        tag.putBoolean("Offhand", offhand);
        tag.putBoolean("Airtime", airtimeScale);
        tag.putBoolean("Saw", saw);
        tag.putInt("SawHits", sawHits);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
}
