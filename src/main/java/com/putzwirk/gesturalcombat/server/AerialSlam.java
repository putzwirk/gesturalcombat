package com.putzwirk.gesturalcombat.server;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID)
public final class AerialSlam {
    private static final double DIVE_SPEED = -2.4;
    private static final double MIN_HEIGHT = 2.0;
    private static final int MAX_DIVE_TICKS = 100;
    private static final int GUARD_TICKS = 40;
    private static final float FALL_KEEP = 0.12f;

    private static final Map<UUID, Dive> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> GUARD = new HashMap<>();

    private AerialSlam() {
    }

    private record Dive(Runnable impact, int expires, double height) {
    }

    public static boolean begin(ServerPlayer player, Runnable impact) {
        if (player.getAbilities().flying || player.isFallFlying() || player.isInWater() || player.isInLava()
                || player.onGround() || player.isPassenger() || player.isSpectator()) {
            return false;
        }
        double height = dropHeight(player);
        if (height < MIN_HEIGHT) {
            return false;
        }
        int expire = player.getServer() == null
                ? MAX_DIVE_TICKS
                : player.getServer().getTickCount() + MAX_DIVE_TICKS;
        ACTIVE.put(player.getUUID(), new Dive(impact, expire, height));
        guard(player, GUARD_TICKS + (int) Math.min(60.0, height * 2.0));
        player.setDeltaMovement(player.getDeltaMovement().x * 0.35, DIVE_SPEED, player.getDeltaMovement().z * 0.35);
        player.hurtMarked = true;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.65f);
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (ACTIVE.isEmpty() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Dive dive = ACTIVE.get(player.getUUID());
        if (dive == null) {
            return;
        }
        if (!player.isAlive() || player.isRemoved()) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        int now = player.getServer() == null ? 0 : player.getServer().getTickCount();
        if (landed(player) || now > dive.expires()) {
            ACTIVE.remove(player.getUUID());
            guard(player, GUARD_TICKS);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.25, 0.0, motion.z * 0.25);
            player.hurtMarked = true;
            dive.impact().run();
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        double diveSpeed = Math.min(DIVE_SPEED - Math.min(1.2, dive.height() * 0.06), motion.y);
        player.setDeltaMovement(motion.x * 0.42, Math.max(DIVE_SPEED - 1.2, diveSpeed), motion.z * 0.42);
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (GUARD.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = player.level().getGameTime();
        Long until = GUARD.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (until < now) {
            GUARD.remove(player.getUUID());
            return;
        }
        event.setDamageMultiplier(FALL_KEEP);
    }

    private static void guard(ServerPlayer player, int ticks) {
        if (GUARD.size() > 128) {
            long now = player.level().getGameTime();
            GUARD.entrySet().removeIf(entry -> entry.getValue() < now);
        }
        GUARD.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    private static boolean landed(ServerPlayer player) {
        return player.onGround() || player.verticalCollision || player.isInWater() || player.isInLava()
                || player.onClimbable();
    }

    private static double dropHeight(ServerPlayer player) {
        if (player.level() == null) {
            return 0.0;
        }
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int step = 1; step <= 40; step++) {
            cursor.set(origin.getX(), origin.getY() - step, origin.getZ());
            BlockState state = player.level().getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }
            if (state.getCollisionShape(player.level(), cursor, CollisionContext.of(player)).isEmpty()) {
                continue;
            }
            return player.getY() - (cursor.getY() + 1);
        }
        return 0.0;
    }
}
