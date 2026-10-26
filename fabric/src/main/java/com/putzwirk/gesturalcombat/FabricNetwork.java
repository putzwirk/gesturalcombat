package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.ModPacket;
import com.putzwirk.gesturalcombat.network.PacketContext;
import com.putzwirk.gesturalcombat.platform.INetworkTransport;
import com.putzwirk.gesturalcombat.platform.ServerRef;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class FabricNetwork implements INetworkTransport {
    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ModNetwork.CHANNEL, (server, player, handler, buf, sender) -> {
            byte[] data = copy(buf);
            server.execute(() -> dispatch(data, PacketContext.server(player)));
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.CHANNEL, (client, handler, buf, sender) -> {
            byte[] data = copy(buf);
            client.execute(() -> dispatch(data, PacketContext.client()));
        });
    }

    private static byte[] copy(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    private static void dispatch(byte[] data, PacketContext context) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int id = buf.readVarInt();
        ModNetwork.dispatch(id, buf, context);
    }

    @Override
    public void sendToServer(ModPacket packet) {
        ClientPlayNetworking.send(ModNetwork.CHANNEL, ModNetwork.write(packet));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ModPacket packet) {
        ServerPlayNetworking.send(player, ModNetwork.CHANNEL, ModNetwork.write(packet));
    }

    @Override
    public void sendToTracking(Entity entity, ModPacket packet) {
        FriendlyByteBuf buf = ModNetwork.write(packet);
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, ModNetwork.CHANNEL, copyOf(buf));
        }
        if (entity instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, ModNetwork.CHANNEL, copyOf(buf));
        }
    }

    @Override
    public void sendToAll(ModPacket packet) {
        if (ServerRef.get() == null) {
            return;
        }
        for (ServerPlayer player : ServerRef.get().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, ModNetwork.CHANNEL, ModNetwork.write(packet));
        }
    }

    @Override
    public void sendNear(Level level, double x, double y, double z, double radius, ModPacket packet) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        FriendlyByteBuf buf = ModNetwork.write(packet);
        for (ServerPlayer player : PlayerLookup.around(serverLevel, new Vec3(x, y, z), radius)) {
            ServerPlayNetworking.send(player, ModNetwork.CHANNEL, copyOf(buf));
        }
    }

    private static FriendlyByteBuf copyOf(FriendlyByteBuf buf) {
        FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer());
        copy.writeBytes(buf, buf.readerIndex(), buf.readableBytes());
        return copy;
    }
}
