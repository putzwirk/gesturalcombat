package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;

public record AbilitySyncPacket(byte[] abilities, byte[] weapons, byte[] audit) implements ModPacket {
    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(abilities == null ? new byte[0] : abilities);
        buf.writeByteArray(weapons == null ? new byte[0] : weapons);
        buf.writeByteArray(audit == null ? new byte[0] : audit);
    }

    public static AbilitySyncPacket decode(FriendlyByteBuf buf) {
        return new AbilitySyncPacket(buf.readByteArray(), buf.readByteArray(), buf.readByteArray());
    }

    public void handle(PacketContext ctx) {
        if (ctx.clientSide()) {
            ClientNet.abilitySync(ConfigPayload.unpack(abilities), ConfigPayload.unpack(weapons),
                    ConfigPayload.unpack(audit));
        }
    }
}
