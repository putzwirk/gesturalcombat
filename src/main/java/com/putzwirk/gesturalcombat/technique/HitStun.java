package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID)
public final class HitStun {
    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();
    private static final Map<UUID, Long> IMMUNE = new HashMap<>();
    private static final int IMMUNITY_TICKS = 6;
    private static final int MAX_CHAIN_TICKS = 16;
    private static final int STAR_MIN_TICKS = 5;

    private HitStun() {
    }

    public static void pin(LivingEntity target, int ticks) {
        pin(target, ticks, false);
    }

    public static void pin(LivingEntity target, int ticks, boolean stars) {
        if (target.level().isClientSide()) {
            return;
        }
        UUID id = target.getUUID();
        long now = target.level().getGameTime();
        if (IMMUNE.getOrDefault(id, 0L) > now) {
            return;
        }
        Anchor existing = ANCHORS.get(id);
        if (ANCHORS.size() > 512) {
            ANCHORS.entrySet().removeIf(entry -> entry.getValue().until < now);
            IMMUNE.entrySet().removeIf(entry -> entry.getValue() < now);
        }
        boolean hadNoAi = existing != null ? existing.hadNoAi : target instanceof Mob mob && mob.isNoAi();
        long chainStart = existing != null && existing.until > now ? existing.start : now;
        long until = now + Math.max(4, ticks);
        if (existing != null && existing.until > until) {
            until = existing.until;
        }
        until = Math.min(until, chainStart + MAX_CHAIN_TICKS);
        if (until <= now) {
            IMMUNE.put(id, now + IMMUNITY_TICKS);
            return;
        }
        boolean showStars = stars || until - now > STAR_MIN_TICKS;
        ANCHORS.put(id, new Anchor(target.level().dimension(), target.getX(), target.getZ(), until, hadNoAi, showStars, chainStart));
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(0.0, motion.y, 0.0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(4, ticks), 4, false, false, false));
        freeze(target);
    }

    public static void release(LivingEntity target) {
        Anchor anchor = ANCHORS.remove(target.getUUID());
        if (anchor != null) {
            restore(target, anchor);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (ANCHORS.isEmpty()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        Anchor anchor = ANCHORS.get(entity.getUUID());
        if (anchor == null) {
            return;
        }
        if (!entity.isAlive() || entity.isRemoved()
                || !entity.level().dimension().equals(anchor.dimension)
                || entity.level().getGameTime() > anchor.until) {
            ANCHORS.remove(entity.getUUID());
            IMMUNE.put(entity.getUUID(), entity.level().getGameTime() + IMMUNITY_TICKS);
            restore(entity, anchor);
            return;
        }
        entity.setPos(anchor.x, entity.getY(), anchor.z);
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
        entity.hurtMarked = true;
        freeze(entity);
        if (anchor.stars && entity.level().getGameTime() % 4 == 0) {
            Fx.stun(entity);
        }
    }

    private static void freeze(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setTarget(null);
        }
        if (entity instanceof Creeper creeper) {
            creeper.setSwellDir(0);
        }
    }

    private static void restore(LivingEntity entity, Anchor anchor) {
        if (entity instanceof Mob mob) {
            mob.setNoAi(anchor.hadNoAi);
        }
    }

    private record Anchor(ResourceKey<Level> dimension, double x, double z, long until, boolean hadNoAi, boolean stars,
                          long start) {
    }
}
