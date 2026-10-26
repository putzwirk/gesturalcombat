package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.technique.AbilitySync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record ConfigRequestPacket() implements ModPacket {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ConfigRequestPacket decode(FriendlyByteBuf buf) {
        return new ConfigRequestPacket();
    }

    public void handle(PacketContext ctx) {
        ServerPlayer player = ctx.sender();
        if (player != null) {
            AbilitySync.send(player);
        }
    }
}
