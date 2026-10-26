package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.network.Envelope;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.ModPacket;
import com.putzwirk.gesturalcombat.network.PacketContext;
import com.putzwirk.gesturalcombat.platform.INetworkTransport;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeNetwork implements INetworkTransport {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(Envelope.TYPE, Envelope.CODEC, (envelope, context) -> context.enqueueWork(() -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(envelope.data()));
            int id = buf.readVarInt();
            PacketContext packetContext = context.flow().getReceptionSide().isServer()
                    ? PacketContext.server((ServerPlayer) context.player())
                    : PacketContext.client();
            ModNetwork.dispatch(id, buf, packetContext);
        }));
    }

    @Override
    public void sendToServer(ModPacket packet) {
        PacketDistributor.sendToServer(Envelope.of(packet));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ModPacket packet) {
        PacketDistributor.sendToPlayer(player, Envelope.of(packet));
    }

    @Override
    public void sendToTracking(Entity entity, ModPacket packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, Envelope.of(packet));
    }

    @Override
    public void sendToAll(ModPacket packet) {
        PacketDistributor.sendToAllPlayers(Envelope.of(packet));
    }

    @Override
    public void sendNear(Level level, double x, double y, double z, double radius, ModPacket packet) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        PacketDistributor.sendToPlayersNear(serverLevel, null, x, y, z, radius, Envelope.of(packet));
    }
}
