package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.mastery.MasteryService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UnlockNodePacket(String nodeId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(nodeId);
    }

    public static UnlockNodePacket decode(FriendlyByteBuf buf) {
        return new UnlockNodePacket(buf.readUtf(64));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                MasteryService.unlock(player, nodeId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
