package com.putzwirk.gesturalcombat.core;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class HandGate {
    private HandGate() {
    }

    public static boolean blocksOffhand(ItemStack mainHand) {
        if (mainHand.isEmpty()) {
            return false;
        }
        return mainHand.has(DataComponents.FOOD) || mainHand.getItem() instanceof BlockItem;
    }
}
