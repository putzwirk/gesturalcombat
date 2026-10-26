package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;

public record OutLockPacket(boolean offhand, boolean locked) implements ModPacket {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(offhand);
        buf.writeBoolean(locked);
    }

    public static OutLockPacket decode(FriendlyByteBuf buf) {
        return new OutLockPacket(buf.readBoolean(), buf.readBoolean());
    }

    public void handle(PacketContext ctx) {
        if (ctx.clientSide()) {
            ClientState.setHidden(offhand, locked);
        }
    }
}
