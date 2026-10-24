package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GestureAnimPacket(int playerId, String animationId, String hand, boolean stop) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(playerId);
        buf.writeUtf(animationId, 96);
        buf.writeUtf(hand == null ? "" : hand, 32);
        buf.writeBoolean(stop);
    }

    public static GestureAnimPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readVarInt();
        String animationId = buf.readUtf(96);
        String handName = buf.readUtf(32);
        boolean stop = buf.readBoolean();
        return new GestureAnimPacket(playerId, animationId, handName.isEmpty() ? null : handName, stop);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = ctx.getSender();
                if (player != null && player.getId() == playerId) {
                    ModNetwork.sendToTracking(player, new GestureAnimPacket(playerId, animationId, hand, stop));
                }
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNet.animation(playerId, animationId, hand, stop));
            }
        });
        ctx.setPacketHandled(true);
    }
}
