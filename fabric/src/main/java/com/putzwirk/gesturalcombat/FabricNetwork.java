package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.network.Envelope;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.ModPacket;
import com.putzwirk.gesturalcombat.network.PacketContext;
import com.putzwirk.gesturalcombat.platform.INetworkTransport;
import com.putzwirk.gesturalcombat.platform.ServerRef;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class FabricNetwork implements INetworkTransport {
    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(Envelope.TYPE, Envelope.CODEC);
        PayloadTypeRegistry.playS2C().register(Envelope.TYPE, Envelope.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(Envelope.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> dispatch(payload, PacketContext.server(player)));
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Envelope.TYPE, (payload, context) ->
                context.client().execute(() -> dispatch(payload, PacketContext.client())));
    }

    private static void dispatch(Envelope envelope, PacketContext context) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(envelope.data()));
        int id = buf.readVarInt();
        ModNetwork.dispatch(id, buf, context);
    }

    @Override
    public void sendToServer(ModPacket packet) {
        ClientPlayNetworking.send(Envelope.of(packet));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ModPacket packet) {
        ServerPlayNetworking.send(player, Envelope.of(packet));
    }

    @Override
    public void sendToTracking(Entity entity, ModPacket packet) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, Envelope.of(packet));
        }
        if (entity instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, Envelope.of(packet));
        }
    }

    @Override
    public void sendToAll(ModPacket packet) {
        if (ServerRef.get() == null) {
            return;
        }
        for (ServerPlayer player : ServerRef.get().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, Envelope.of(packet));
        }
    }

    @Override
    public void sendNear(Level level, double x, double y, double z, double radius, ModPacket packet) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (ServerPlayer player : PlayerLookup.around(serverLevel, new Vec3(x, y, z), radius)) {
            ServerPlayNetworking.send(player, Envelope.of(packet));
        }
    }
}
