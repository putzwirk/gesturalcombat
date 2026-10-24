package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.client.ClientNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MasterySyncPacket(CompoundTag data) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static MasterySyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new MasterySyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNet.mastery(data)));
        context.get().setPacketHandled(true);
    }
}
