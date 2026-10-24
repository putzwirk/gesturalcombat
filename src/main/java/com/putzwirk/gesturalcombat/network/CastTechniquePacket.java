package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.server.GestureCastService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CastTechniquePacket(String techniqueId, GestureShape shape, float confidence, int swipe, boolean offhand) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(techniqueId);
        buf.writeEnum(shape);
        buf.writeFloat(confidence);
        buf.writeByte(Math.max(-1, Math.min(1, swipe)));
        buf.writeBoolean(offhand);
    }

    public static CastTechniquePacket decode(FriendlyByteBuf buf) {
        return new CastTechniquePacket(buf.readUtf(64), buf.readEnum(GestureShape.class), buf.readFloat(), buf.readByte(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                GestureCastService.cast(player, techniqueId, shape, confidence, swipe, offhand);
            }
        });
        ctx.setPacketHandled(true);
    }
}
