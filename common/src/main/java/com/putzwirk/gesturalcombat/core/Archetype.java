package com.putzwirk.gesturalcombat.core;

import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import javax.annotation.Nullable;

import java.util.Locale;

public enum Archetype {
    SWORD("sword", 0x62C9FF),
    AXE("axe", 0xFF9440),
    SPEAR("spear", 0x6BFFA8),
    SHIELD("shield", 0x6BFFA8),
    BOW("bow", 0xFFC45A);

    private final String id;
    private final int color;

    Archetype(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return 0xFF000000 | color;
    }

    public record Hit(Archetype archetype, String source) {
    }

    @Nullable
    public static Archetype of(ItemStack stack) {
        Hit hit = resolve(stack);
        return hit == null ? null : hit.archetype();
    }

    @Nullable
    public static Hit resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Archetype configured = com.putzwirk.gesturalcombat.weapon.WeaponProfiles.archetype(stack);
        if (configured != null) {
            return new Hit(configured, "profile");
        }
        if (isShield(stack)) {
            return new Hit(SHIELD, "shield");
        }
        Item item = stack.getItem();
        if (item instanceof ProjectileWeaponItem) {
            return new Hit(BOW, "projectile");
        }
        if (item instanceof TridentItem) {
            return new Hit(SPEAR, "trident");
        }
        Archetype combat = fromBetterCombat(stack);
        if (combat != null) {
            return new Hit(combat, "bettercombat");
        }
        if (stack.is(ItemTags.SWORDS)) {
            return new Hit(SWORD, "tags");
        }
        if (stack.is(ItemTags.AXES)) {
            return new Hit(AXE, "tags");
        }
        Archetype named = fromName(stack);
        return named == null ? null : new Hit(named, "name");
    }

    private static final String[] SWORD_TOKENS = {
            "sword", "longsword", "blade", "katana", "saber", "sabre", "rapier", "cutlass", "dagger",
            "scimitar", "claymore", "machete", "falchion", "sickle"
    };
    private static final String[] AXE_TOKENS = {
            "axe", "greataxe", "hatchet", "cleaver", "scythe", "poleaxe"
    };
    private static final String[] SPEAR_TOKENS = {
            "spear", "lance", "pike", "halberd", "glaive", "trident", "naginata", "partisan",
            "harpoon", "javelin", "staff", "quarterstaff", "battlestaff", "polearm"
    };
    private static final String[] BOW_TOKENS = {
            "bow", "longbow", "shortbow", "crossbow"
    };

    public static boolean hasToken(String value, String token) {
        if (value == null || token == null || token.isEmpty()) {
            return false;
        }
        int at = value.indexOf(token);
        while (at >= 0) {
            boolean start = at == 0 || value.charAt(at - 1) == '_' || value.charAt(at - 1) == '/';
            int end = at + token.length();
            boolean stop = end == value.length() || value.charAt(end) == '_' || value.charAt(end) == '/';
            if (start && stop) {
                return true;
            }
            at = value.indexOf(token, at + 1);
        }
        return false;
    }

    private static boolean anyToken(String path, String[] tokens) {
        for (String token : tokens) {
            if (hasToken(path, token)) {
                return true;
            }
        }
        return false;
    }

    public static boolean dealsDamage(ItemStack stack) {
        net.minecraft.world.item.component.ItemAttributeModifiers modifiers =
                stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return false;
        }
        for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Archetype active(Player player) {
        Archetype main = of(player.getMainHandItem());
        if (main != null) {
            return main;
        }
        return of(player.getOffhandItem());
    }

    @Nullable
    public static Archetype forHand(Player player, net.minecraft.world.InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> of(player.getMainHandItem());
            case OFF_HAND -> of(player.getOffhandItem());
        };
    }

    public static boolean isShield(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ShieldItem) {
            return true;
        }
        if (stack.getItem() instanceof BlockItem) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && hasToken(key.getPath().toLowerCase(Locale.ROOT), "shield");
    }

    @Nullable
    private static Archetype fromName(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return null;
        }
        if (stack.getItem() instanceof BlockItem || !dealsDamage(stack)) {
            return null;
        }
        String path = key.getPath().toLowerCase(Locale.ROOT);
        if (anyToken(path, SWORD_TOKENS)) {
            return SWORD;
        }
        if (anyToken(path, AXE_TOKENS)) {
            return AXE;
        }
        if (anyToken(path, SPEAR_TOKENS)) {
            return SPEAR;
        }
        if (anyToken(path, BOW_TOKENS)) {
            return BOW;
        }
        return null;
    }

    @Nullable
    private static Archetype fromBetterCombat(ItemStack stack) {
        String family = BetterCombatBridge.family(BetterCombatBridge.category(stack));
        if (family == null) {
            return null;
        }
        return switch (family) {
            case "sword" -> SWORD;
            case "axe" -> AXE;
            case "spear" -> SPEAR;
            default -> null;
        };
    }
}
