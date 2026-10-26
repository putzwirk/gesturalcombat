package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.entity.ModEntities;
import com.putzwirk.gesturalcombat.entity.ThrownShieldEntity;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.ShieldLock;
import com.putzwirk.gesturalcombat.technique.Fx;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShieldThrowEffect implements TechniqueEffect {
    private final double range;
    private final int maxTargets;
    private final int lockTicks;

    public ShieldThrowEffect(double range, int maxTargets, int lockTicks) {
        this.range = range;
        this.maxTargets = Math.max(1, maxTargets);
        this.lockTicks = Math.max(1, lockTicks);
    }

    @Override
    public void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique) {
        ItemStack shield = stack.copy();
        if (shield.isEmpty()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(eye);
            double distance = to.length();
            if (distance > range || distance < 0.1) {
                continue;
            }
            if (to.normalize().dot(look) < 0.86) {
                continue;
            }
            targets.add(entity);
        }
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        if (targets.size() > maxTargets) {
            targets = new ArrayList<>(targets.subList(0, maxTargets));
        }

        Vec3 impact = targets.isEmpty()
                ? eye.add(look.scale(range * 0.75))
                : targets.get(0).position().add(0.0, targets.get(0).getBbHeight() * 0.5, 0.0);

        ThrownShieldEntity visual = new ThrownShieldEntity(ModEntities.THROWN_SHIELD.get(), level,
                eye.add(0.0, -0.25, 0.0), look.scale(1.8), shield);
        visual.setYRot((float) Math.toDegrees(Math.atan2(look.x, look.z)));
        visual.setYHeadRot(visual.getYRot());
        level.addFreshEntity(visual);
        GestureScheduler.schedule(level, 30, visual::discard);

        List<LivingEntity> hitTargets = targets;
        GestureScheduler.schedule(level, 6, () -> {
            if (!player.isAlive() || player.isRemoved() || player.level() != level) {
                return;
            }
            float damage = (float) (EffectSupport.damageOf(player, technique) * 0.35);
            for (LivingEntity target : hitTargets) {
                if (!target.isAlive()) {
                    continue;
                }
                EffectSupport.hit(player, target, damage, technique, 0.1, 0.0, true);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, false, true));
            }

            Vec3 center = impact;
            if (!hitTargets.isEmpty()) {
                LivingEntity first = hitTargets.get(0);
                if (first.isAlive()) {
                    center = first.position().add(0.0, first.getBbHeight() * 0.5, 0.0);
                }
            }
            Fx.shieldBurst(player, center, 3.4);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.9f);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(center, center).inflate(4.0), e -> e != player && e.isAlive() && !e.isSpectator())) {
                Vec3 push = entity.position().subtract(center);
                push = new Vec3(push.x, 0.0, push.z);
                if (push.lengthSqr() < 1.0e-4) {
                    push = look.scale(-1.0);
                }
                push = push.normalize().scale(1.25);
                entity.push(push.x, 0.35, push.z);
                entity.hurtMarked = true;
            }
            visual.discard();
        });

        player.getCooldowns().addCooldown(shield.getItem(), 120);
        ShieldLock.lock(player, lockTicks);
        EffectSupport.sound(level, player, SoundEvents.TRIDENT_THROW, 1.0f, 1.2f);
    }
}
