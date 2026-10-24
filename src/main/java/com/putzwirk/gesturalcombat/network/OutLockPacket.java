package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OutLockPacket(boolean offhand, boolean locked) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(offhand);
        buf.writeBoolean(locked);
    }

    public static OutLockPacket decode(FriendlyByteBuf buf) {
        return new OutLockPacket(buf.readBoolean(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientState.setHidden(offhand, locked)));
        context.get().setPacketHandled(true);
    }
}
