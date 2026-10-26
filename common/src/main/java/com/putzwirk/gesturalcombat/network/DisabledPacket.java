package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record DisabledPacket(List<String> players) implements ModPacket {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(players.size());
        for (String id : players) {
            buf.writeUtf(id);
        }
    }

    public static DisabledPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> players = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            players.add(buf.readUtf());
        }
        return new DisabledPacket(players);
    }

    public void handle(PacketContext ctx) {
        if (!ctx.clientSide()) {
            return;
        }
        ClientState.setDisabledPlayers(players);
    }
}
