package com.putzwirk.gesturalcombat.entity;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Gesturalcombat.MODID);

    public static final RegistryObject<EntityType<ThrownShieldEntity>> THROWN_SHIELD =
            ENTITIES.register("thrown_shield", () -> EntityType.Builder
                    .<ThrownShieldEntity>of(ThrownShieldEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("thrown_shield"));

    private ModEntities() {
    }
}
