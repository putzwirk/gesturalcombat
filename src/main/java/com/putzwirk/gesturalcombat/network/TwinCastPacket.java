package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TwinCastPacket(int casterId, String techniqueId, int swipe, boolean offhand) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(casterId);
        buf.writeUtf(techniqueId);
        buf.writeByte(Math.max(-1, Math.min(1, swipe)));
        buf.writeBoolean(offhand);
    }

    public static TwinCastPacket decode(FriendlyByteBuf buf) {
        return new TwinCastPacket(buf.readVarInt(), buf.readUtf(64), buf.readByte(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNet.twinCast(casterId, techniqueId, swipe, offhand)));
        context.get().setPacketHandled(true);
    }
}
