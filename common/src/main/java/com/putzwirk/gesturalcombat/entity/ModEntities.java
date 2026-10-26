package com.putzwirk.gesturalcombat.entity;

import com.putzwirk.gesturalcombat.platform.Platform;
import com.putzwirk.gesturalcombat.platform.Reg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static Reg<EntityType<ThrownShieldEntity>> THROWN_SHIELD;
    public static Reg<EntityType<ThrownChakramEntity>> THROWN_CHAKRAM;

    private ModEntities() {
    }

    public static void registerAll() {
        THROWN_SHIELD = Platform.get().registerEntity("thrown_shield", () -> EntityType.Builder
                .<ThrownShieldEntity>of(ThrownShieldEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build("thrown_shield"));

        THROWN_CHAKRAM = Platform.get().registerEntity("thrown_chakram", () -> EntityType.Builder
                .<ThrownChakramEntity>of(ThrownChakramEntity::new, MobCategory.MISC)
                .sized(0.6f, 0.6f)
                .clientTrackingRange(10)
                .updateInterval(1)
                .build("thrown_chakram"));
    }
}
