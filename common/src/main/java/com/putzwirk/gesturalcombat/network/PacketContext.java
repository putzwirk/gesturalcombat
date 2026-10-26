package com.putzwirk.gesturalcombat.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record PacketContext(boolean clientSide, ServerPlayer sender) {
    public static PacketContext server(ServerPlayer sender) {
        return new PacketContext(false, sender);
    }

    public static PacketContext client() {
        return new PacketContext(true, null);
    }
}
