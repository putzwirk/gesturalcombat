package com.putzwirk.gesturalcombat.core;

import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
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

    public String nameKey() {
        return "archetype.gesturalcombat." + id;
    }

    public String descKey() {
        return "archetype.gesturalcombat." + id + ".desc";
    }

    @Nullable
    public static Archetype of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Archetype configured = com.putzwirk.gesturalcombat.weapon.WeaponProfiles.archetype(stack);
        if (configured != null) {
            return configured;
        }
        if (isShield(stack)) {
            return SHIELD;
        }
        Item item = stack.getItem();
        if (item instanceof BowItem) {
            return BOW;
        }
        if (item instanceof TridentItem) {
            return SPEAR;
        }
        Archetype combat = fromBetterCombat(stack);
        if (combat != null) {
            return combat;
        }
        if (item instanceof SwordItem || stack.is(ItemTags.SWORDS)) {
            return SWORD;
        }
        if (item instanceof AxeItem || stack.is(ItemTags.AXES)) {
            return AXE;
        }
        return fromName(stack);
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
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && key.getPath().toLowerCase(Locale.ROOT).contains("shield");
    }

    @Nullable
    private static Archetype fromName(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return null;
        }
        String path = key.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("bow")) {
            return BOW;
        }
        if (contains(path, "sword", "blade", "katana", "saber", "sabre", "rapier", "cutlass", "dagger",
                "scimitar", "claymore", "machete", "falchion", "longsword", "sickle")) {
            return SWORD;
        }
        if (contains(path, "axe", "hatchet", "cleaver", "scythe", "poleaxe", "greataxe")) {
            return AXE;
        }
        if (contains(path, "spear", "lance", "pike", "halberd", "glaive", "trident", "naginata", "partisan",
                "harpoon", "javelin", "staff", "quarterstaff", "battlestaff", "polearm")) {
            return SPEAR;
        }
        return null;
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
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
