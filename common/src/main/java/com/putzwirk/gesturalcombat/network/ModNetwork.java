package com.putzwirk.gesturalcombat.network;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.platform.Platform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModNetwork {
    public static final ResourceLocation CHANNEL = new ResourceLocation(Gesturalcombat.MODID, "main");

    private static final List<Function<FriendlyByteBuf, ? extends ModPacket>> DECODERS = new ArrayList<>();
    private static final Map<Class<?>, Integer> IDS = new HashMap<>();

    private ModNetwork() {
    }

    public static synchronized <T extends ModPacket> void register(Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        IDS.put(type, DECODERS.size());
        DECODERS.add(decoder);
    }

    public static void registerAll() {
        if (!DECODERS.isEmpty()) {
            return;
        }
        register(CastTechniquePacket.class, CastTechniquePacket::decode);
        register(GestureAnimPacket.class, GestureAnimPacket::decode);
        register(CastEffectPacket.class, CastEffectPacket::decode);
        register(AbilitySyncPacket.class, AbilitySyncPacket::decode);
        register(FxPacket.class, FxPacket::decode);
        register(FirePatchPacket.class, FirePatchPacket::decode);
        register(TwinCastPacket.class, TwinCastPacket::decode);
        register(OutLockPacket.class, OutLockPacket::decode);
        register(SaltoPacket.class, SaltoPacket::decode);
        register(ConfigRequestPacket.class, ConfigRequestPacket::decode);
        register(DisabledPacket.class, DisabledPacket::decode);
    }

    public static FriendlyByteBuf write(ModPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        Integer id = IDS.get(packet.getClass());
        if (id == null) {
            throw new IllegalStateException("unregistered packet " + packet.getClass().getName());
        }
        buf.writeVarInt(id);
        packet.encode(buf);
        return buf;
    }

    public static void dispatch(int id, FriendlyByteBuf buf, PacketContext ctx) {
        if (id < 0 || id >= DECODERS.size()) {
            Gesturalcombat.LOGGER.warn("gesturalcombat dropped unknown packet id {}", id);
            return;
        }
        DECODERS.get(id).apply(buf).handle(ctx);
    }

    public static void sendToServer(ModPacket packet) {
        Platform.net().sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, ModPacket packet) {
        Platform.net().sendToPlayer(player, packet);
    }

    public static void sendToTracking(Entity entity, ModPacket packet) {
        Platform.net().sendToTracking(entity, packet);
    }

    public static void sendToAll(ModPacket packet) {
        Platform.net().sendToAll(packet);
    }

    public static void sendNear(Level level, double x, double y, double z, double radius, ModPacket packet) {
        Platform.net().sendNear(level, x, y, z, radius, packet);
    }
}
