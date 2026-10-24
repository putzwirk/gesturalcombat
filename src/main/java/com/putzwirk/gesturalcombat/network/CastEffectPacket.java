package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CastEffectPacket(int casterId, String techniqueId, int cooldownTicks, int swipe, boolean offhand, String cooldownKey) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(casterId);
        buf.writeUtf(techniqueId);
        buf.writeVarInt(Math.max(0, cooldownTicks));
        buf.writeByte(Math.max(-1, Math.min(1, swipe)));
        buf.writeBoolean(offhand);
        buf.writeUtf(cooldownKey == null ? "" : cooldownKey, 128);
    }

    public static CastEffectPacket decode(FriendlyByteBuf buf) {
        int casterId = buf.readVarInt();
        String techniqueId = buf.readUtf(64);
        int cooldownTicks = buf.readVarInt();
        int swipe = buf.readByte();
        boolean offhand = buf.readBoolean();
        String key = buf.readUtf(128);
        return new CastEffectPacket(casterId, techniqueId, cooldownTicks, swipe, offhand, key);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNet.castEffect(casterId, techniqueId, cooldownTicks, swipe, offhand, cooldownKey)));
        context.get().setPacketHandled(true);
    }
}
