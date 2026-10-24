package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
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
    private static final int SWORD_TINT = 0xD6F4FF;
    private static final int AXE_TINT = 0xFFC48C;
    private static final int SPEAR_TINT = 0xC9FFDD;
    private static final int SHIELD_TINT = 0xD6FFE2;
    private static final int BOW_TINT = 0xFFE3A6;
    private static final int DEFAULT_TINT = 0xE8F6FF;

    private static final Random RANDOM = new Random();

    private Fx() {
    }

    private record Style(FxType type, boolean heavy, int life) {
    }

    public static void slash(ServerPlayer player, Technique technique, ItemStack stack, Hitbox hitbox) {
        Style style = styleFor(technique, hitbox);
        Vec3 at = origin(player, hitbox, style.type);
        int tint = tint(technique, stack);
        float scale = scaleOf(hitbox, style.type);
        int swipe = CastContext.swipe(player);
        float roll = rollOf(style.type, technique.shape(), swipe);
        float spin = style.type == FxType.SLASH360 ? 26.0f : 0.0f;
        send(player, style.type, at, roll, spin, scale, tint, style.life);
        if (style.heavy) {
            send(player, FxType.BURST, at, 0.0f, 0.0f, scale * 0.8f, tint, 7);
        }
    }

    public static void stab(ServerPlayer player, Technique technique, ItemStack stack, Hitbox hitbox) {
        Vec3 at = origin(player, hitbox, FxType.STAB);
        int tint = tint(technique, stack);
        float scale = scaleOf(hitbox, FxType.STAB);
        send(player, FxType.STAB, at, rollOf(FxType.STAB, technique.shape(), CastContext.swipe(player)), 0.0f, scale, tint, 8);
    }

    public static void impact(ServerPlayer player, Technique technique, ItemStack stack, Hitbox hitbox) {
        Vec3 at = origin(player, hitbox, FxType.RING);
        int tint = tint(technique, stack);
        float scale = Math.max(0.8f, scaleOf(hitbox, FxType.RING));
        send(player, FxType.RING, at, 0.0f, 0.0f, scale * 1.15f, tint, 9);
        send(player, FxType.BURST, at, 0.0f, 0.0f, scale, tint, 7);
    }

    public static void ring(ServerPlayer player, Technique technique, ItemStack stack, Vec3 at, double radius) {
        int tint = tint(technique, stack);
        float scale = (float) Math.max(0.9, Math.min(2.6, radius * 0.5));
        send(player, FxType.RING, at, 0.0f, 0.0f, scale, tint, 10);
        send(player, FxType.BURST, at.add(0.0, 0.15, 0.0), 0.0f, 0.0f, scale * 0.7f, tint, 8);
    }

    public static void special(ServerPlayer player, Technique technique, WeaponSpecial special, Hitbox hitbox) {
        Vec3 at = origin(player, hitbox, FxType.SLASH360);
        int tint = tintForFamily(special.particles(), tint(technique, player.getMainHandItem()));
        float scale = Math.max(0.9f, scaleOf(hitbox, FxType.SLASH360));
        FxType primary = switch (hitbox.shape()) {
            case SPHERE -> FxType.SLASH360;
            case VERTICAL_ARC -> FxType.STAB;
            case BOX -> FxType.STAB;
            case ARC -> FxType.SLASH180;
        };
        float spin = primary == FxType.SLASH360 ? 26.0f : 0.0f;
        send(player, primary, at, rollOf(primary, technique.shape(), CastContext.swipe(player)), spin, scale * 1.15f, tint, 9);
        send(player, familyBloom(special.particles()), at, 0.0f, 0.0f, scale, tint, 9);
        if (hitbox.shape() == Hitbox.Shape.SPHERE) {
            send(player, FxType.RING, at, 0.0f, 0.0f, scale * 0.9f, tint, 10);
        }
    }

    public static void stun(LivingEntity entity) {
        int tint = 0xFFF0B0;
        Vec3 at = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() + 0.25, entity.getZ());
        ModNetwork.sendToTracking(entity, packet(FxType.BURST, at, 0.0f, 0.0f, 0.55f, tint, 8));
    }

    public static void dash(ServerPlayer player) {
        int tint = tint(player.getMainHandItem());
        Vec3 at = new Vec3(player.getX(), player.getY() + 0.6, player.getZ());
        send(player, FxType.BOLT, at, 0.0f, 0.0f, 0.7f, tint, 7);
    }

    public static void shieldBurst(ServerPlayer player, Vec3 at, double radius) {
        int tint = SHIELD_TINT;
        float scale = (float) Math.max(0.9, Math.min(2.6, radius * 0.5));
        send(player, FxType.RING, at, 0.0f, 0.0f, scale, tint, 9);
        send(player, FxType.BURST, at, 0.0f, 0.0f, scale * 0.8f, tint, 8);
    }

    private static FxType familyBloom(String family) {
        String value = family == null ? "" : family.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "flame", "soul", "soul_fire_flame", "crit" -> FxType.BURST;
            case "electric_spark" -> FxType.BOLT;
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

    private static Style styleFor(Technique technique, Hitbox hitbox) {
        GestureShape shape = technique.shape();
        Archetype archetype = technique.archetype();
        if (archetype == Archetype.SHIELD) {
            return new Style(FxType.RING, true, 9);
        }
        if (hitbox.shape() == Hitbox.Shape.BOX) {
            return new Style(FxType.STAB, false, 8);
        }
        if (shape == GestureShape.CIRCLE || hitbox.shape() == Hitbox.Shape.SPHERE) {
            return new Style(FxType.SLASH360, true, 9);
        }
        if (shape == GestureShape.LINE_H || shape == GestureShape.LINE_V_UP || shape == GestureShape.LINE_V_DOWN) {
            return new Style(FxType.STAB, false, 8);
        }
        boolean heavy = archetype == Archetype.AXE || isHeavy(technique);
        return new Style(heavy ? FxType.SLASH180 : FxType.SLASH90, heavy, 8);
    }

    private static boolean isHeavy(Technique technique) {
        String id = technique.id();
        return id.contains("execute") || id.contains("cleave") || id.contains("slam")
                || id.contains("quake") || id.contains("skyfall") || id.contains("rend");
    }

    private static float rollOf(FxType type, GestureShape shape, int swipe) {
        if (type == FxType.STAB) {
            return switch (shape) {
                case LINE_H -> swipe >= 0 ? 90.0f : -90.0f;
                case LINE_V_DOWN -> 180.0f;
                default -> 0.0f;
            };
        }
        return switch (type) {
            case SLASH90 -> swipe >= 0 ? 0.0f : 180.0f;
            case SLASH180 -> swipe >= 0 ? -5.0f : 180.0f;
            case SLASH360 -> 180.0f;
            default -> 0.0f;
        };
    }

    private static float scaleOf(Hitbox hitbox, FxType type) {
        double factor = type == FxType.STAB ? 0.58 : 0.42;
        double min = type == FxType.STAB ? 0.85 : 0.55;
        double max = type == FxType.STAB ? 2.6 : 2.2;
        return (float) Math.max(min, Math.min(max, hitbox.range() * factor));
    }

    private static Vec3 origin(ServerPlayer player, Hitbox hitbox, FxType type) {
        Vec3 facing = Hitbox.facing(player.getYRot());
        Vec3 at = switch (type) {
            case STAB -> player.getEyePosition().add(player.getLookAngle().scale(Math.max(0.8, hitbox.range() * 0.6)));
            case SLASH360 -> player.getEyePosition().add(0.0, -0.3, 0.0);
            default -> player.getEyePosition().add(facing.scale(Math.max(0.7, hitbox.range() * 0.55)));
        };
        return at.add(0.0, hitbox.verticalOffset() - 0.1, 0.0);
    }

    private static int tint(Technique technique, ItemStack stack) {
        Archetype archetype = technique.archetype();
        if (archetype == null) {
            return DEFAULT_TINT;
        }
        return archetype.color() & 0xFFFFFF;
    }

    private static int tint(ItemStack stack) {
        Archetype archetype = Archetype.of(stack);
        return archetype == null ? DEFAULT_TINT : archetype.color() & 0xFFFFFF;
    }

    private static void send(ServerPlayer player, FxType type, Vec3 at, float roll, float spin, float scale, int rgb, int life) {
        ModNetwork.sendToTracking(player, packet(type, at, roll, spin, scale, rgb, life));
    }

    private static FxPacket packet(FxType type, Vec3 at, float roll, float spin, float scale, int rgb, int life) {
        float vx = (RANDOM.nextFloat() - 0.5f) * 0.08f;
        float vy = (RANDOM.nextFloat() - 0.5f) * 0.06f;
        float vz = (RANDOM.nextFloat() - 0.5f) * 0.08f;
        return new FxPacket(type, at.x, at.y, at.z, vx, vy, vz, roll, spin, scale, rgb, life);
    }
}
