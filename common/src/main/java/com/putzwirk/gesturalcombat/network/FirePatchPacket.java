package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.FireClientPatches;
import net.minecraft.network.FriendlyByteBuf;

public record FirePatchPacket(double x, double y, double z, double radius, int durationTicks) implements ModPacket {
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

    public void handle(PacketContext ctx) {
        if (ctx.clientSide()) {
            FireClientPatches.add(x, y, z, radius, durationTicks);
        }
    }
}
