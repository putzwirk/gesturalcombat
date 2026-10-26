package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.network.ModPacket;
import com.putzwirk.gesturalcombat.network.PacketContext;
import com.putzwirk.gesturalcombat.platform.INetworkTransport;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ForgeNetwork implements INetworkTransport {
    private static final String VERSION = "15";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ModNetwork.CHANNEL, () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, Envelope.class, Envelope::encode, Envelope::decode, Envelope::handle);
    }

    @Override
    public void sendToServer(ModPacket packet) {
        CHANNEL.sendToServer(Envelope.of(packet));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ModPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), Envelope.of(packet));
    }

    @Override
    public void sendToTracking(Entity entity, ModPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), Envelope.of(packet));
    }

    @Override
    public void sendToAll(ModPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), Envelope.of(packet));
    }

    @Override
    public void sendNear(Level level, double x, double y, double z, double radius, ModPacket packet) {
        CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(x, y, z, radius, level.dimension())), Envelope.of(packet));
    }

    public record Envelope(byte[] data) {
        static Envelope of(ModPacket packet) {
            return new Envelope(ByteBufUtil.getBytes(ModNetwork.write(packet)));
        }

        public static void encode(Envelope envelope, FriendlyByteBuf buf) {
            buf.writeByteArray(envelope.data);
        }

        public static Envelope decode(FriendlyByteBuf buf) {
            return new Envelope(buf.readByteArray());
        }

        public static void handle(Envelope envelope, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(envelope.data));
                int id = buf.readVarInt();
                PacketContext packetContext = context.getDirection().getReceptionSide().isServer()
                        ? PacketContext.server(context.getSender())
                        : PacketContext.client();
                ModNetwork.dispatch(id, buf, packetContext);
            });
            context.setPacketHandled(true);
        }
    }
}
