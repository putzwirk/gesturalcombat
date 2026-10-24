package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.DashMovement;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class SkirmishEffect implements TechniqueEffect {
    private static final int WINDOW = 8;
    private static final double SEARCH_BONUS = 4.0;
    private static final double MAX_SEARCH_ANGLE = 75.0;
    private static final double MAX_DASH_DISTANCE = 6.0;
    private static final double DASH_STEP = 0.30;
    private static final int DASH_TICKS = 12;
    private static final double FREE_DASH_DISTANCE = 4.6;

    private final double range;
    private final double arcDegrees;
    private final double damageMultiplier;
    private final double dash;
    private final int retreatDelay;

    public SkirmishEffect(double range, double arcDegrees, float damageMultiplier, double dash, int retreatDelay) {
        this(range, arcDegrees, (double) damageMultiplier, dash, retreatDelay);
    }

    public SkirmishEffect(double range, double arcDegrees, double damageMultiplier, double dash, int retreatDelay) {
        this.range = range;
        this.arcDegrees = arcDegrees;
        this.damageMultiplier = damageMultiplier;
        this.dash = dash;
        this.retreatDelay = Math.max(1, retreatDelay);
    }

    @Override
    public int windupTicks() {
        return 3;
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        Hitbox hitbox = EffectSupport.hitboxOf(player, technique, stack, Hitbox.arc(range, arcDegrees));
        double reach = hitbox.range();
        double stopDistance = Math.max(2.5, reach * 0.7);
        float damage = (float) (EffectSupport.damageOf(player, technique, stack)
                * AbilityConfigs.damageMultiplier(technique, stack, damageMultiplier));
        LivingEntity target = player.onGround() ? bestTarget(player, reach + SEARCH_BONUS) : null;

        int windup = AbilityConfigs.windupTicks(technique, stack);
        int ticks;
        if (target != null) {
            Vec3 destination = DashMovement.stopPoint(player, target, stopDistance, MAX_DASH_DISTANCE);
            ticks = DashMovement.run(player, level, destination, DASH_TICKS, windup + 2, DASH_STEP);
        } else {
            Vec3 destination = DashMovement.forwardPoint(player, Math.min(MAX_DASH_DISTANCE, FREE_DASH_DISTANCE));
            ticks = DashMovement.run(player, level, destination, DASH_TICKS, windup + 2, DASH_STEP);
        }
        scheduleStats(level, player, technique, stack, hitbox, damage, ticks + windup, target);

        Vec3 retreat = player.getLookAngle();
        Vec3 flat = new Vec3(retreat.x, 0.0, retreat.z);
        if (!player.onGround() || flat.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 back = flat.normalize().scale(-1.0);
        GestureScheduler.schedule(level, retreatDelay, () -> {
            if (player.isAlive() && !player.isRemoved() && player.level() == level && player.onGround()) {
                DashMovement.retreat(player, level, back, dash, 7);
            }
        });
    }

    private static void scheduleStats(Level level, ServerPlayer player, Technique technique, ItemStack stack, Hitbox hitbox, float damage, int delay, @Nullable LivingEntity focus) {
        GestureScheduler.schedule(level, delay, () -> {
            if (!player.isAlive() || player.isRemoved() || player.level() != level) {
                return;
            }
            double force = AbilityConfigs.knockback(technique, stack, 0.05);
            double lift = AbilityConfigs.lift(technique, stack, 0.0);
            for (LivingEntity target : EffectSupport.targets(player, level, hitbox, focus)) {
                EffectSupport.hit(player, target, damage, technique, force, lift, true);
            }
            EffectSupport.sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
        });
    }

    private static LivingEntity bestTarget(ServerPlayer player, double searchRange) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(searchRange + 2.0),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = center(entity).subtract(eye);
            double distance = to.length();
            if (distance < 0.1 || distance > searchRange) {
                continue;
            }
            double dot = look.dot(to.normalize());
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
            if (angle > MAX_SEARCH_ANGLE) {
                continue;
            }
            double score = distance + angle * 0.35;
            if (score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    private static Vec3 flatTo(ServerPlayer player, LivingEntity target) {
        Vec3 to = target.position().subtract(player.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        if (flat.lengthSqr() < 1.0e-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return flat.normalize();
    }

    private static Vec3 center(LivingEntity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    private static double flatDistance(ServerPlayer player, LivingEntity entity) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dz * dz) - entity.getBbWidth() * 0.5;
    }
}
