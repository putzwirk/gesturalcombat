package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.platform.IPlatform;
import com.putzwirk.gesturalcombat.platform.Reg;
import com.putzwirk.gesturalcombat.world.FractureBlockEntity;
import com.putzwirk.gesturalcombat.world.ModTerrain;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.nio.file.Path;
import java.util.function.Supplier;

public final class NeoForgePlatform implements IPlatform {
    private final DeferredRegister<EntityType<?>> entities =
            DeferredRegister.create(Registries.ENTITY_TYPE, Gesturalcombat.MODID);
    private final DeferredRegister<ParticleType<?>> particles =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Gesturalcombat.MODID);
    private final DeferredRegister<Block> blocks =
            DeferredRegister.create(Registries.BLOCK, Gesturalcombat.MODID);
    private final DeferredRegister<BlockEntityType<?>> blockEntities =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gesturalcombat.MODID);

    public void attach(IEventBus bus) {
        entities.register(bus);
        particles.register(bus);
        blocks.register(bus);
        blockEntities.register(bus);
    }

    @Override
    public String name() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Reg<T> registerEntity(String name, Supplier<T> supplier) {
        DeferredHolder<EntityType<?>, EntityType<?>> holder =
                entities.register(name, () -> (EntityType<?>) supplier.get());
        return new Reg<>(() -> (T) holder.get());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Reg<SimpleParticleType> registerParticle(String name, Supplier<SimpleParticleType> supplier) {
        DeferredHolder<ParticleType<?>, ParticleType<?>> holder =
                particles.register(name, supplier::get);
        return new Reg<>(() -> (SimpleParticleType) holder.get());
    }

    @Override
    public Reg<Block> registerBlock(String name, Supplier<Block> supplier) {
        DeferredHolder<Block, Block> holder = blocks.register(name, supplier);
        return new Reg<>(holder::get);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Reg<BlockEntityType<FractureBlockEntity>> registerFractureBlockEntity() {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> holder = blockEntities.register(
                "terrain_fracture",
                () -> BlockEntityType.Builder.of(FractureBlockEntity::new, ModTerrain.FRACTURE.get()).build(null));
        return new Reg<>(() -> (BlockEntityType<FractureBlockEntity>) holder.get());
    }
}
