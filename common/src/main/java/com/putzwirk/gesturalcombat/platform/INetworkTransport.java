package com.putzwirk.gesturalcombat.platform;

import com.putzwirk.gesturalcombat.network.ModPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface INetworkTransport {
    void sendToServer(ModPacket packet);

    void sendToPlayer(ServerPlayer player, ModPacket packet);

    void sendToTracking(Entity entity, ModPacket packet);

    void sendToAll(ModPacket packet);

    void sendNear(Level level, double x, double y, double z, double radius, ModPacket packet);
}
