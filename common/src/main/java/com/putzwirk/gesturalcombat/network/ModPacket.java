package com.putzwirk.gesturalcombat.network;

import net.minecraft.network.FriendlyByteBuf;

public interface ModPacket {
    void encode(FriendlyByteBuf buf);

    void handle(PacketContext ctx);
}
