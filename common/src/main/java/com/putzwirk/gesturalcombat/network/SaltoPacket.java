package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;

public record SaltoPacket(int casterId, int ticks) implements ModPacket {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(casterId);
        buf.writeVarInt(ticks);
    }

    public static SaltoPacket decode(FriendlyByteBuf buf) {
        return new SaltoPacket(buf.readVarInt(), buf.readVarInt());
    }

    public void handle(PacketContext ctx) {
        if (ctx.clientSide()) {
            ClientNet.salto(casterId, ticks);
        }
    }
}
