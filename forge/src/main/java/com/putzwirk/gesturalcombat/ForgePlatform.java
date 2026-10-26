package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.platform.IPlatform;
import com.putzwirk.gesturalcombat.world.FractureBlockEntity;
import com.putzwirk.gesturalcombat.world.ModTerrain;
import com.putzwirk.gesturalcombat.platform.Reg;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.nio.file.Path;
import java.util.function.Supplier;

public final class ForgePlatform implements IPlatform {
    private final DeferredRegister<EntityType<?>> entities =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE.key(), Gesturalcombat.MODID);
    private final DeferredRegister<ParticleType<?>> particles =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE.key(), Gesturalcombat.MODID);
    private final DeferredRegister<Block> blocks =
            DeferredRegister.create(BuiltInRegistries.BLOCK.key(), Gesturalcombat.MODID);
    private final DeferredRegister<BlockEntityType<?>> blockEntities =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), Gesturalcombat.MODID);

    public void attach(IEventBus bus) {
        entities.register(bus);
        particles.register(bus);
        blocks.register(bus);
        blockEntities.register(bus);
    }

    @Override
    public String name() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return net.minecraftforge.fml.ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDirectory() {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Reg<T> registerEntity(String name, Supplier<T> supplier) {
        RegistryObject<T> object = (RegistryObject<T>) entities.register(name, () -> (EntityType<?>) supplier.get());
        return new Reg<>(object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Reg<SimpleParticleType> registerParticle(String name, Supplier<SimpleParticleType> supplier) {
        RegistryObject<SimpleParticleType> object =
                (RegistryObject<SimpleParticleType>) (RegistryObject<?>) particles.register(name, supplier);
        return new Reg<>(object);
    }

    @Override
    public Reg<Block> registerBlock(String name, Supplier<Block> supplier) {
        return new Reg<>(blocks.register(name, supplier));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Reg<BlockEntityType<FractureBlockEntity>> registerFractureBlockEntity() {
        RegistryObject<BlockEntityType<FractureBlockEntity>> object =
                (RegistryObject<BlockEntityType<FractureBlockEntity>>) (RegistryObject<?>) blockEntities.register(
                        "terrain_fracture",
                        () -> BlockEntityType.Builder.of(FractureBlockEntity::new, ModTerrain.FRACTURE.get()).build(null));
        return new Reg<>(object);
    }

    static ResourceLocation id(String name) {
        return new ResourceLocation(Gesturalcombat.MODID, name);
    }
}
