package com.putzwirk.gesturalcombat.platform;

import com.putzwirk.gesturalcombat.world.FractureBlockEntity;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.nio.file.Path;
import java.util.function.Supplier;

public interface IPlatform {
    String name();

    boolean isModLoaded(String modId);

    Path configDirectory();

    <T> Reg<T> registerEntity(String name, Supplier<T> supplier);

    Reg<SimpleParticleType> registerParticle(String name, Supplier<SimpleParticleType> supplier);

    Reg<Block> registerBlock(String name, Supplier<Block> supplier);

    Reg<BlockEntityType<FractureBlockEntity>> registerFractureBlockEntity();
}
