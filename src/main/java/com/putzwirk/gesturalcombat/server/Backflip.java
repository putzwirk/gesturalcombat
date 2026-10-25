package com.putzwirk.gesturalcombat.server;

import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.SaltoPacket;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.TechniqueImmunity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class Backflip {
    public static final int TICKS = 14;

    private Backflip() {
    }

    public static void launch(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0e-4) {
            flat = new Vec3(0.0, 0.0, 1.0);
        }
        flat = flat.normalize();
        player.setDeltaMovement(-flat.x * 0.95, 0.62, -flat.z * 0.95);
        player.hurtMarked = true;
        TechniqueImmunity.grant(player, TICKS);
        ModNetwork.sendToTracking(player, new SaltoPacket(player.getId(), TICKS));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.7f);
        GestureScheduler.schedule(player.level(), TICKS + 8, () -> {
            if (player.isAlive() && !player.isRemoved()) {
                player.fallDistance = 0.0f;
            }
        });
    }
}
