package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.mastery.MasteryCapability;
import com.putzwirk.gesturalcombat.mastery.PlayerMastery;
import com.putzwirk.gesturalcombat.skill.Passive;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import net.minecraft.core.particles.ParticleOptions;
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
        double config = Config.INSTANCE.damageMultiplier.get();
        return (float) (base * passive * weapon * config);
    }

    public static double reachOf(ServerPlayer player, Technique technique, double baseRange) {
        return reachOf(player, technique, player.getMainHandItem(), baseRange);
    }

    public static double reachOf(ServerPlayer player, Technique technique, ItemStack stack, double baseRange) {
        return baseRange * reachFactor(player, technique, stack);
    }

    public static double reachFactor(ServerPlayer player, Technique technique, ItemStack stack) {
        float passive = MasteryCapability.get(player).passive(technique.archetype(), Passive.RANGE);
        return (1.0f + passive) * WeaponProfiles.reachMultiplier(stack);
    }

    public static Hitbox scaleHitbox(ServerPlayer player, Technique technique, ItemStack stack, Hitbox box) {
        float passive = MasteryCapability.get(player).passive(technique.archetype(), Passive.RANGE);
        double factor = 1.0 + passive;
        double absolute = WeaponProfiles.reach(stack);
        double range = absolute > 0.0
                ? absolute * factor
                : box.range() * factor * WeaponProfiles.reachMultiplier(stack);
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
        return Math.max(4, (int) Math.round(baseTicks * config * (1.0f - reduction) * attackSpeedFactor * weapon));
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

    public static void hit(ServerPlayer player, LivingEntity target, float damage, Technique technique, double knockback, double lift) {
        hit(player, target, damage, technique, knockback, lift, false);
    }

    public static void hit(ServerPlayer player, LivingEntity target, float damage, Technique technique, double knockback, double lift, boolean pin) {
        target.invulnerableTime = 0;
        ItemStack weapon = onHitWeapon(player, technique);
        boolean delegated = !weapon.isEmpty()
                && com.putzwirk.gesturalcombat.compat.SimplySwordsBridge.applyHit(weapon, target, player, damage);
        if (delegated) {
            target.invulnerableTime = Math.min(target.invulnerableTime, 2);
        } else if (target.hurt(player.damageSources().playerAttack(player), damage)) {
            target.invulnerableTime = Math.min(target.invulnerableTime, 2);
        }
        if (pin) {
            int interval = AbilityConfigs.hitInterval(technique);
            HitStun.pin(target, interval > 0 ? Math.max(2, interval - 2) : 4);
        } else if (knockback <= 0.3 && lift <= 0.0) {
            HitStun.pin(target, 4);
        } else {
            HitStun.release(target);
            if (knockback > 0.0) {
                Vec3 push = target.position().subtract(player.position());
                Vec3 flat = new Vec3(push.x, 0.0, push.z);
                if (flat.lengthSqr() < 1.0E-4) {
                    flat = player.getLookAngle();
                }
                flat = flat.normalize().scale(knockback);
                target.push(flat.x, Math.max(0.0, lift * 0.25), flat.z);
                target.hurtMarked = true;
            }
            if (lift > 0.0) {
                target.push(0.0, lift, 0.0);
                target.hurtMarked = true;
            }
        }
        Level level = player.level();
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.55f, 1.15f + player.getRandom().nextFloat() * 0.15f);
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

    public static void slash(Level level, ServerPlayer player, Hitbox hitbox, Technique technique, ItemStack stack) {
        Fx.slash(player, technique, stack, hitbox);
    }

    public static void ringParticles(Level level, ServerPlayer player, double radius, Technique technique, ItemStack stack) {
        Fx.ring(player, technique, stack, player.position().add(0.0, 0.25, 0.0), radius);
    }

    public static void sound(Level level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void swingSound(Level level, ServerPlayer player, Technique technique) {
        switch (technique.archetype()) {
            case SWORD -> sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.0f + player.getRandom().nextFloat() * 0.2f);
            case AXE -> sound(level, player, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.8f);
            case SPEAR -> sound(level, player, SoundEvents.TRIDENT_RIPTIDE_1, 0.6f, 1.4f);
            case SHIELD -> sound(level, player, SoundEvents.SHIELD_BLOCK, 1.0f, 1.0f);
            case BOW -> sound(level, player, SoundEvents.ARROW_SHOOT, 0.8f, 1.0f);
        }
    }
}
