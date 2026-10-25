package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.mastery.MasteryCapability;
import com.putzwirk.gesturalcombat.mastery.PlayerMastery;
import com.putzwirk.gesturalcombat.skill.Passive;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import net.minecraft.core.particles.ParticleOptions;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class EffectSupport {
    private static final double MIN_ATTACK_SPEED_FACTOR = 0.5;

    private EffectSupport() {
    }

    public static List<LivingEntity> targets(ServerPlayer player, Level level, Hitbox hitbox, @Nullable LivingEntity focus) {
        List<LivingEntity> targets = new ArrayList<>(hitbox.query(player, level));
        if (focus == null || !focus.isAlive() || targets.contains(focus)) {
            return targets;
        }
        double distance = Math.sqrt(focus.getBoundingBox().distanceToSqr(player.getEyePosition()));
        if (distance <= hitbox.range() + 1.0) {
            targets.add(focus);
        }
        return targets;
    }

    public static Vec3 look(ServerPlayer player) {
        return player.getLookAngle();
    }

    public static double yawDegrees(ServerPlayer player) {
        return player.getYRot();
    }

    public static float damageOf(ServerPlayer player, Technique technique) {
        return damageOf(player, technique, player.getMainHandItem());
    }

    public static float damageOf(ServerPlayer player, Technique technique, ItemStack stack) {
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float passive = 1.0f + MasteryCapability.get(player).passive(technique.archetype(), Passive.DAMAGE);
        double weapon = WeaponProfiles.damageMultiplier(stack);
        double weight = com.putzwirk.gesturalcombat.weapon.WeaponRules.damageFactor(stack);
        double config = Config.INSTANCE.damageMultiplier.get();
        return (float) (base * passive * weapon * weight * config);
    }

    public static double reachOf(ServerPlayer player, Technique technique, double baseRange) {
        return reachOf(player, technique, player.getMainHandItem(), baseRange);
    }

    public static double reachOf(ServerPlayer player, Technique technique, ItemStack stack, double baseRange) {
        return baseRange * reachFactor(player, technique, stack);
    }

    public static double weaponReach(ItemStack stack) {
        double absolute = WeaponProfiles.reach(stack);
        if (absolute > 0.0) {
            return absolute;
        }
        double combat = com.putzwirk.gesturalcombat.compat.BetterCombatBridge.attackRange(stack);
        return Math.max(0.0, combat);
    }

    public static double reachFactor(ServerPlayer player, Technique technique, ItemStack stack) {
        float passive = MasteryCapability.get(player).passive(technique.archetype(), Passive.RANGE);
        return (1.0f + passive) * WeaponProfiles.reachMultiplier(stack);
    }

    public static Hitbox scaleHitbox(ServerPlayer player, Technique technique, ItemStack stack, Hitbox box) {
        float passive = MasteryCapability.get(player).passive(technique.archetype(), Passive.RANGE);
        double factor = 1.0 + passive;
        double absolute = weaponReach(stack);
        double family = com.putzwirk.gesturalcombat.weapon.WeaponRules.reachFactor(stack);
        double range = absolute > 0.0
                ? absolute * factor * family
                : box.range() * factor * WeaponProfiles.reachMultiplier(stack) * family;
        return box.withRange(range);
    }

    public static Hitbox defaultHitbox(Technique technique, double range, double arcDegrees) {
        if (technique.shape() == com.putzwirk.gesturalcombat.core.GestureShape.LINE_V_UP
                || technique.shape() == com.putzwirk.gesturalcombat.core.GestureShape.LINE_V_DOWN) {
            return Hitbox.verticalArc(range, Math.max(120.0, arcDegrees), Math.max(2.4, range));
        }
        return Hitbox.arc(range, arcDegrees);
    }

    public static Hitbox hitboxOf(ServerPlayer player, Technique technique, ItemStack stack, Hitbox fallback) {
        Hitbox scaled = scaleHitbox(player, technique, stack, AbilityConfigs.hitbox(technique, stack, fallback));
        if (Config.INSTANCE.debugHitboxes.get()) {
            scaled.outline(player);
        }
        return scaled;
    }

    public static int cooldownOf(ServerPlayer player, Technique technique) {
        return cooldownOf(player, technique, player.getMainHandItem());
    }

    public static int cooldownOf(ServerPlayer player, Technique technique, ItemStack stack) {
        return specialCooldown(player, technique, stack, AbilityConfigs.cooldownTicks(technique, stack));
    }

    public static int specialCooldown(ServerPlayer player, Technique technique, ItemStack stack, int baseTicks) {
        PlayerMastery mastery = MasteryCapability.get(player);
        float reduction = Math.min(0.6f, mastery.passive(technique.archetype(), Passive.COOLDOWN));
        double config = Config.INSTANCE.cooldownMultiplier.get();
        double attackSpeedFactor = attackSpeedFactor(player, technique);
        double weapon = WeaponProfiles.cooldownMultiplier(stack);
        double family = com.putzwirk.gesturalcombat.weapon.WeaponRules.cooldownFactor(stack);
        return Math.max(4, (int) Math.round(baseTicks * config * (1.0f - reduction) * attackSpeedFactor * weapon * family));
    }

    private static double attackSpeedFactor(ServerPlayer player, Technique technique) {
        double reference = referenceAttackSpeed(technique.archetype());
        if (reference <= 0.0) {
            return 1.0;
        }
        double speed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        if (speed <= 0.05) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(MIN_ATTACK_SPEED_FACTOR, reference / speed));
    }

    private static double referenceAttackSpeed(Archetype archetype) {
        if (archetype == null) {
            return 0.0;
        }
        return switch (archetype) {
            case SWORD -> 1.6;
            case AXE -> 1.0;
            case SPEAR -> 1.1;
            case SHIELD, BOW -> 0.0;
        };
    }

    public static boolean withinCone(ServerPlayer player, LivingEntity target, double range, double arcDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 toTarget = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(eye);
        double distance = toTarget.length();
        if (distance > range + target.getBbWidth() * 0.5 || distance < 0.05) {
            return false;
        }
        Vec3 flat = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z).normalize();
        Vec3 targetFlat = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (targetFlat.lengthSqr() < 1.0E-4) {
            return true;
        }
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, flat.dot(targetFlat.normalize())))));
        return angle <= arcDegrees * 0.5;
    }

    @Nullable
    public static LivingEntity targetInFront(ServerPlayer player, double range, double maxAngleDegrees) {
        Vec3 flat = Hitbox.facing(player.getYRot());
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range + 2.0), e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = entity.position().subtract(player.position());
            Vec3 toFlat = new Vec3(to.x, 0.0, to.z);
            double distance = toFlat.length() - entity.getBbWidth() * 0.5;
            if (distance > range) {
                continue;
            }
            if (toFlat.lengthSqr() < 1.0E-6) {
                return entity;
            }
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, flat.dot(toFlat.normalize())))));
            if (angle > maxAngleDegrees) {
                continue;
            }
            double score = Math.max(0.0, distance) + angle * 0.03;
            if (score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    public static List<LivingEntity> cone(ServerPlayer player, double range, double arcDegrees) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range + 2.0), e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (withinCone(player, entity, range, arcDegrees)) {
                out.add(entity);
            }
        }
        return out;
    }

    public static List<LivingEntity> area(ServerPlayer player, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius + 1.0), e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (entity.position().distanceTo(player.position()) <= radius + entity.getBbWidth() * 0.5) {
                out.add(entity);
            }
        }
        return out;
    }

    private static final java.util.Map<String, Long> LAST_DELEGATED = new java.util.HashMap<>();
    private static final int DELEGATED_COOLDOWN = 8;

    private static boolean delegate(ServerPlayer player, LivingEntity target, ItemStack weapon, float damage) {
        String key = player.getUUID() + "|" + target.getUUID();
        long now = target.level().getGameTime();
        Long last = LAST_DELEGATED.get(key);
        if (last != null && now - last < DELEGATED_COOLDOWN) {
            return false;
        }
        if (LAST_DELEGATED.size() > 512) {
            LAST_DELEGATED.entrySet().removeIf(entry -> now - entry.getValue() > 200);
        }
        if (!com.putzwirk.gesturalcombat.compat.SimplySwordsBridge.applyHit(weapon, target, player, damage)) {
            return false;
        }
        LAST_DELEGATED.put(key, now);
        return true;
    }

    public static void hit(ServerPlayer player, LivingEntity target, float damage, Technique technique, double knockback, double lift) {
        hit(player, target, damage, technique, knockback, lift, false);
    }

    public static void hit(ServerPlayer player, LivingEntity target, float damage, Technique technique, double knockback, double lift, boolean pin) {
        target.invulnerableTime = 0;
        ItemStack weapon = onHitWeapon(player, technique);
        boolean delegated = !weapon.isEmpty() && delegate(player, target, weapon, damage);
        boolean landed = delegated;
        if (delegated) {
            target.invulnerableTime = Math.min(target.invulnerableTime, 2);
        } else if (target.hurt(player.damageSources().playerAttack(player), damage)) {
            target.invulnerableTime = Math.min(target.invulnerableTime, 2);
            landed = true;
        }
        if (landed) {
            Fx.hit(player, technique, weapon, target.getBoundingBox().getCenter(), 1.1);
        }
        ItemStack held = Archetype.of(player.getMainHandItem()) == technique.archetype()
                ? player.getMainHandItem() : player.getOffhandItem();
        int stun = com.putzwirk.gesturalcombat.weapon.WeaponRules.hitStunTicks(held);
        if (pin) {
            int interval = AbilityConfigs.hitInterval(technique);
            HitStun.pin(target, Math.max(stun, interval > 0 ? Math.max(2, interval - 2) : 4));
        } else if (knockback <= 0.3 && lift <= 0.0) {
            HitStun.pin(target, stun);
        } else {
            HitStun.release(target);
            Vec3 flat = target.position().subtract(player.position());
            flat = new Vec3(flat.x, 0.0, flat.z);
            if (flat.lengthSqr() < 1.0E-4) {
                flat = player.getLookAngle();
            }
            Vec3 shove = flat.normalize().scale(knockback);
            if (lift > 0.0) {
                target.push(shove.x, lift, shove.z);
                target.hurtMarked = true;
            } else if (knockback > 0.0) {
                HitStun.pin(target, stun);
                GestureScheduler.schedule(player.level(), stun + 1, () -> {
                    if (target.isAlive() && !target.isRemoved()) {
                        target.push(shove.x, 0.1, shove.z);
                        target.hurtMarked = true;
                    }
                });
            }
        }
        Level level = player.level();
        boolean heavy = com.putzwirk.gesturalcombat.weapon.WeaponRules.heavy(held);
        boolean light = com.putzwirk.gesturalcombat.weapon.WeaponRules.light(held);
        float pitch = (heavy ? 0.70f : light ? 1.42f : 1.05f) + player.getRandom().nextFloat() * 0.10f;
        float volume = heavy ? 0.95f : light ? 0.45f : 0.65f;
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, volume, pitch);
        if (heavy) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS, 0.30f, 1.35f + player.getRandom().nextFloat() * 0.15f);
        }
    }

    private static ItemStack onHitWeapon(ServerPlayer player, Technique technique) {
        ItemStack weapon = player.getMainHandItem();
        if (Archetype.of(weapon) != technique.archetype()) {
            ItemStack offhand = player.getOffhandItem();
            if (Archetype.of(offhand) != technique.archetype()) {
                return ItemStack.EMPTY;
            }
            weapon = offhand;
        }
        if (weapon.isEmpty() || WeaponProfiles.profile(weapon) == null) {
            return ItemStack.EMPTY;
        }
        return weapon;
    }

    public static void particles(Level level, ParticleOptions options, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(options, x, y, z, count, dx, dy, dz, speed);
        }
    }

    public static void ringParticles(Level level, ServerPlayer player, double radius, Technique technique, ItemStack stack) {
        Fx.ring(player, technique, stack, player.position().add(0.0, 0.25, 0.0), radius);
    }

    public static void groundWave(Level level, ServerPlayer player, Technique technique, ItemStack stack, double radius) {
        double reach = Math.max(2.0, Math.min(5.0, radius));
        Vec3 at = player.position().add(0.0, 0.05, 0.0);
        Fx.wave(player, technique, stack, at, reach);
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return;
        }
        ParticleOptions debris = groundDebris(level, at);
        int rings = 5;
        for (int step = 0; step < rings; step++) {
            int wave = step;
            double ringRadius = reach * (0.22 + 0.78 * wave / (double) (rings - 1));
            int age = wave * 2;
            GestureScheduler.schedule(level, age, () -> {
                int count = 20;
                for (int i = 0; i < count; i++) {
                    double angle = Math.PI * 2.0 * i / count + wave * 0.23;
                    double dx = Math.cos(angle) * ringRadius;
                    double dz = Math.sin(angle) * ringRadius;
                    server.sendParticles(debris, at.x + dx, at.y + 0.12, at.z + dz, 1, 0.04, 0.02, 0.04, 0.02);
                    if ((i + wave) % 4 == 0) {
                        server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                                at.x + dx, at.y + 0.28, at.z + dz, 1, 0.12, 0.05, 0.12, 0.01);
                    }
                }
            });
        }
        sound(level, player, SoundEvents.GENERIC_EXPLODE, 0.45f, 0.55f);
        for (LivingEntity entity : Hitbox.sphere(reach * 0.95).query(player, level)) {
            Vec3 away = entity.position().subtract(player.position());
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            if (flat.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 shove = flat.normalize().scale(0.32);
            entity.push(shove.x, 0.12, shove.z);
            entity.hurtMarked = true;
        }
    }

    private static ParticleOptions groundDebris(Level level, Vec3 at) {
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(at.x, at.y - 0.2, at.z);
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            state = level.getBlockState(pos.below());
        }
        if (state.isAir()) {
            return net.minecraft.core.particles.ParticleTypes.CLOUD;
        }
        return new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state);
    }

    public static void sound(Level level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void swingSound(Level level, ServerPlayer player, Technique technique) {
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        boolean heavy = com.putzwirk.gesturalcombat.weapon.WeaponRules.heavy(stack);
        boolean light = com.putzwirk.gesturalcombat.weapon.WeaponRules.light(stack);
        float basePitch = heavy ? 0.62f : light ? 1.34f : 1.0f;
        float pitch = basePitch + player.getRandom().nextFloat() * 0.12f;
        float volume = heavy ? 1.0f : light ? 0.65f : 0.85f;
        switch (technique.archetype()) {
            case SWORD -> sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, volume, pitch);
            case AXE -> sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, volume * 1.05f, pitch * 0.92f);
            case SPEAR -> sound(level, player, SoundEvents.TRIDENT_RIPTIDE_1, heavy ? 0.55f : 0.7f, heavy ? 1.15f : 1.45f);
            case SHIELD -> sound(level, player, SoundEvents.SHIELD_BLOCK, 1.0f, 1.0f);
            case BOW -> sound(level, player, SoundEvents.ARROW_SHOOT, 0.8f, 1.0f);
        }
    }
}
