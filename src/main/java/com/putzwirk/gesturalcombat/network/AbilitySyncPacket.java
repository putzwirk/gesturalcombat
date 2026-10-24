package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AbilitySyncPacket(String abilities, String weapons) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(abilities == null ? "" : abilities, 262144);
        buf.writeUtf(weapons == null ? "" : weapons, 262144);
    }

    public static AbilitySyncPacket decode(FriendlyByteBuf buf) {
        return new AbilitySyncPacket(buf.readUtf(262144), buf.readUtf(262144));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNet.abilitySync(abilities, weapons)));
        context.get().setPacketHandled(true);
    }
}
