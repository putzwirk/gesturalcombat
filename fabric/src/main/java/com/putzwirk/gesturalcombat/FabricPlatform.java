package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.platform.IPlatform;
import com.putzwirk.gesturalcombat.platform.Reg;
import com.putzwirk.gesturalcombat.world.FractureBlockEntity;
import com.putzwirk.gesturalcombat.world.ModTerrain;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.nio.file.Path;
import java.util.function.Supplier;

public final class FabricPlatform implements IPlatform {
    @Override
    public String name() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Reg<T> registerEntity(String name, Supplier<T> supplier) {
        T value = supplier.get();
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id(name), (EntityType<?>) value);
        return Reg.of(value);
    }

    @Override
    public Reg<SimpleParticleType> registerParticle(String name, Supplier<SimpleParticleType> supplier) {
        return Reg.of(Registry.register(BuiltInRegistries.PARTICLE_TYPE, id(name), supplier.get()));
    }

    @Override
    public Reg<Block> registerBlock(String name, Supplier<Block> supplier) {
        return Reg.of(Registry.register(BuiltInRegistries.BLOCK, id(name), supplier.get()));
    }

    @Override
    public Reg<BlockEntityType<FractureBlockEntity>> registerFractureBlockEntity() {
        BlockEntityType<FractureBlockEntity> type = FabricBlockEntityTypeBuilder
                .create(FractureBlockEntity::new, ModTerrain.FRACTURE.get()).build();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("terrain_fracture"), type);
        return Reg.of(type);
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Gesturalcombat.MODID, name);
    }
}
