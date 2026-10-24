package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.fx.FxClient;
import com.putzwirk.gesturalcombat.particle.FxType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FxPacket(FxType type, double x, double y, double z, float vx, float vy, float vz,
                       float roll, float spin, float scale, int rgb, int life) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(type.ordinal());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(vx);
        buf.writeFloat(vy);
        buf.writeFloat(vz);
        buf.writeFloat(roll);
        buf.writeFloat(spin);
        buf.writeFloat(scale);
        buf.writeInt(rgb);
        buf.writeByte(life);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(
                com.putzwirk.gesturalcombat.particle.ModParticles.byOrdinal(buf.readByte()),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readByte());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FxClient.spawn(type, x, y, z, vx, vy, vz, roll, spin, scale, rgb, life)));
        context.get().setPacketHandled(true);
    }
}
