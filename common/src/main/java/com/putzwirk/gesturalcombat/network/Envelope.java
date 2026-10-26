package com.putzwirk.gesturalcombat.network;

import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record Envelope(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<Envelope> TYPE = new CustomPacketPayload.Type<>(ModNetwork.CHANNEL);
    public static final StreamCodec<FriendlyByteBuf, Envelope> CODEC =
            CustomPacketPayload.codec(Envelope::write, Envelope::new);

    public static Envelope of(ModPacket packet) {
        return new Envelope(ByteBufUtil.getBytes(ModNetwork.write(packet)));
    }

    public Envelope(FriendlyByteBuf buf) {
        this(buf.readByteArray());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByteArray(data);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
