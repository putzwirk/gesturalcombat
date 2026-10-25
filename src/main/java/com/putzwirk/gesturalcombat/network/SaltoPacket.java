package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SaltoPacket(int casterId, int ticks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(casterId);
        buf.writeVarInt(ticks);
    }

    public static SaltoPacket decode(FriendlyByteBuf buf) {
        return new SaltoPacket(buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNet.salto(casterId, ticks)));
        context.get().setPacketHandled(true);
    }
}
