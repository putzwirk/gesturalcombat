package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;

public record GestureAnimPacket(int playerId, String animationId, String hand, boolean stop) implements ModPacket {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(playerId);
        buf.writeUtf(animationId, 96);
        buf.writeUtf(hand == null ? "" : hand, 32);
        buf.writeBoolean(stop);
    }

    public static GestureAnimPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readVarInt();
        String animationId = buf.readUtf(96);
        String handName = buf.readUtf(32);
        boolean stop = buf.readBoolean();
        return new GestureAnimPacket(playerId, animationId, handName.isEmpty() ? null : handName, stop);
    }

    public void handle(PacketContext ctx) {
        if (ctx.clientSide()) {
            ClientNet.animation(playerId, animationId, hand, stop);
        } else if (ctx.sender() != null && ctx.sender().getId() == playerId) {
            ModNetwork.sendToTracking(ctx.sender(), new GestureAnimPacket(playerId, animationId, hand, stop));
        }
    }
}
