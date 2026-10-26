package com.putzwirk.gesturalcombat.world;

import com.putzwirk.gesturalcombat.platform.Platform;
import com.putzwirk.gesturalcombat.platform.Reg;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModTerrain {
    public static Reg<Block> FRACTURE;
    public static Reg<BlockEntityType<FractureBlockEntity>> FRACTURE_ENTITY;

    private ModTerrain() {
    }

    public static void registerAll() {
        FRACTURE = Platform.get().registerBlock("terrain_fracture", FractureBlock::new);
        FRACTURE_ENTITY = Platform.get().registerFractureBlockEntity();
    }
}
