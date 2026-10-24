package com.putzwirk.gesturalcombat.weapon;

import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.Technique;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class CooldownKeys {
    private CooldownKeys() {
    }

    public static String of(ItemStack stack, GestureShape shape, Technique technique) {
        WeaponSpecial special = WeaponProfiles.special(stack, shape);
        if (special == null) {
            return technique.id();
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return "special:" + (id == null ? "unknown" : id) + ":" + shape;
    }
}
