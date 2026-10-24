package com.putzwirk.gesturalcombat.mastery;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class MasteryCapability {
    public static final Capability<PlayerMastery> MASTERY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private static final PlayerMastery EMPTY = new PlayerMastery();

    private MasteryCapability() {
    }

    public static PlayerMastery get(Player player) {
        return player.getCapability(MASTERY).orElse(EMPTY);
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerMastery mastery = new PlayerMastery();
        private final LazyOptional<PlayerMastery> optional = LazyOptional.of(() -> mastery);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == MASTERY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return mastery.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            mastery.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
