package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;

public record TwinCastPacket(int casterId, String techniqueId, int swipe, boolean offhand) implements ModPacket {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(casterId);
        buf.writeUtf(techniqueId);
        buf.writeByte(Math.max(-1, Math.min(1, swipe)));
        buf.writeBoolean(offhand);
    }

    public static TwinCastPacket decode(FriendlyByteBuf buf) {
        return new TwinCastPacket(buf.readVarInt(), buf.readUtf(64), buf.readByte(), buf.readBoolean());
    }

    public void handle(PacketContext ctx) {
        if (ctx.clientSide()) {
            ClientNet.twinCast(casterId, techniqueId, swipe, offhand);
        }
    }
}
