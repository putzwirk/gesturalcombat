package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.FireClientPatches;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FirePatchPacket(double x, double y, double z, double radius, int durationTicks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(radius);
        buf.writeVarInt(durationTicks);
    }

    public static FirePatchPacket decode(FriendlyByteBuf buf) {
        return new FirePatchPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FireClientPatches.add(x, y, z, radius, durationTicks)));
        context.get().setPacketHandled(true);
    }
}
