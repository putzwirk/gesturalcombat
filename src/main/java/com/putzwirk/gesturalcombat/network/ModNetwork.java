package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "9";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Gesturalcombat.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private static int nextId = 0;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, MasterySyncPacket.class, MasterySyncPacket::encode, MasterySyncPacket::decode, MasterySyncPacket::handle);
        CHANNEL.registerMessage(nextId++, CastTechniquePacket.class, CastTechniquePacket::encode, CastTechniquePacket::decode, CastTechniquePacket::handle);
        CHANNEL.registerMessage(nextId++, UnlockNodePacket.class, UnlockNodePacket::encode, UnlockNodePacket::decode, UnlockNodePacket::handle);
        CHANNEL.registerMessage(nextId++, GestureAnimPacket.class, GestureAnimPacket::encode, GestureAnimPacket::decode, GestureAnimPacket::handle);
        CHANNEL.registerMessage(nextId++, CastEffectPacket.class, CastEffectPacket::encode, CastEffectPacket::decode, CastEffectPacket::handle);
        CHANNEL.registerMessage(nextId++, AbilitySyncPacket.class, AbilitySyncPacket::encode, AbilitySyncPacket::decode, AbilitySyncPacket::handle);
        CHANNEL.registerMessage(nextId++, FxPacket.class, FxPacket::encode, FxPacket::decode, FxPacket::handle);
        CHANNEL.registerMessage(nextId++, FirePatchPacket.class, FirePatchPacket::encode, FirePatchPacket::decode, FirePatchPacket::handle);
        CHANNEL.registerMessage(nextId++, OutLockPacket.class, OutLockPacket::encode, OutLockPacket::decode, OutLockPacket::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToTracking(Entity entity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static void sendToAll(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendNear(net.minecraft.world.level.Level level, double x, double y, double z, double radius, Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                x, y, z, radius, level.dimension())), message);
    }
}
