package com.putzwirk.gesturalcombat.world;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModTerrain {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Gesturalcombat.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Gesturalcombat.MODID);

    public static final RegistryObject<Block> FRACTURE = BLOCKS.register("terrain_fracture", FractureBlock::new);
    public static final RegistryObject<BlockEntityType<FractureBlockEntity>> FRACTURE_ENTITY =
            BLOCK_ENTITIES.register("terrain_fracture",
                    () -> BlockEntityType.Builder.of(FractureBlockEntity::new, FRACTURE.get()).build(null));

    private ModTerrain() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
