package com.putzwirk.gesturalcombat.core;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class HandGate {
    private HandGate() {
    }

    public static boolean blocksOffhand(ItemStack mainHand) {
        if (mainHand.isEmpty()) {
            return false;
        }
        return mainHand.isEdible() || mainHand.getItem() instanceof BlockItem;
    }
}
