package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.technique.AbilitySync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ConfigRequestPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ConfigRequestPacket decode(FriendlyByteBuf buf) {
        return new ConfigRequestPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                AbilitySync.send(player);
            }
        });
        context.get().setPacketHandled(true);
    }
}
