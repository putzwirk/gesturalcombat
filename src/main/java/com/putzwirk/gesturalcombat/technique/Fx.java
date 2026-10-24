package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.network.FxPacket;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.particle.FxType;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.weapon.WeaponSpecial;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Random;

public final class Fx {
    private static final int SHIELD_TINT = 0xD6FFE2;
    private static final int DEFAULT_TINT = 0xE8F6FF;

    private static final Random RANDOM = new Random();

    private Fx() {
    }

    public static void ring(ServerPlayer player, Technique technique, ItemStack stack, Vec3 at, double radius) {
        int tint = tint(technique, stack);
        float scale = (float) Math.max(0.9, Math.min(2.6, radius * 0.5));
        send(player, FxType.RING, at, scale, tint, 10);
        send(player, FxType.BURST, at.add(0.0, 0.15, 0.0), scale * 0.7f, tint, 8);
    }

    public static void hit(ServerPlayer player, Technique technique, ItemStack stack, Vec3 at, double radius) {
        int tint = tint(technique, stack);
        float scale = (float) Math.max(0.7, Math.min(2.2, radius * 0.45));
        send(player, FxType.BURST, at, scale, tint, 8);
    }

    public static void special(ServerPlayer player, Technique technique, WeaponSpecial special, Hitbox hitbox) {
        int tint = tintForFamily(special.particles(), tint(technique, player.getMainHandItem()));
        float scale = Math.max(0.9f, (float) Math.min(2.4, hitbox.range() * 0.45));
        Vec3 at = player.getEyePosition().add(0.0, -0.2, 0.0);
        send(player, familyBloom(special.particles()), at, scale, tint, 9);
        if (hitbox.shape() == Hitbox.Shape.SPHERE) {
            send(player, FxType.RING, at, scale * 0.9f, tint, 10);
        }
    }

    public static void stun(LivingEntity entity) {
        Vec3 at = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() + 0.25, entity.getZ());
        ModNetwork.sendToTracking(entity, packet(FxType.BURST, at, 0.55f, 0xFFF0B0, 8));
    }

    public static void shieldBurst(ServerPlayer player, Vec3 at, double radius) {
        float scale = (float) Math.max(0.9, Math.min(2.6, radius * 0.5));
        send(player, FxType.RING, at, scale, SHIELD_TINT, 9);
        send(player, FxType.BURST, at, scale * 0.8f, SHIELD_TINT, 8);
    }

    private static FxType familyBloom(String family) {
        String value = family == null ? "" : family.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "snowflake", "end_rod", "enchanted_hit", "witch" -> FxType.RING;
            default -> FxType.BURST;
        };
    }

    public static int tintForFamily(String family, int fallback) {
        String value = family == null ? "" : family.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "flame" -> 0xFFB05A;
            case "soul", "soul_fire_flame" -> 0xC79BFF;
            case "crit" -> 0xFF8A7A;
            case "electric_spark" -> 0xA8D8FF;
            case "snowflake" -> 0xC8F6FF;
            case "enchanted_hit" -> 0xD0A8FF;
            case "witch" -> 0xA8FF9A;
            case "end_rod" -> 0xFFF0B0;
            default -> fallback;
        };
    }

    private static int tint(Technique technique, ItemStack stack) {
        Archetype archetype = technique.archetype();
        return archetype == null ? DEFAULT_TINT : archetype.color() & 0xFFFFFF;
    }

    private static void send(ServerPlayer player, FxType type, Vec3 at, float scale, int rgb, int life) {
        ModNetwork.sendToTracking(player, packet(type, at, scale, rgb, life));
    }

    private static FxPacket packet(FxType type, Vec3 at, float scale, int rgb, int life) {
        return new FxPacket(type, at.x, at.y, at.z, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, scale, rgb, life);
    }
}
