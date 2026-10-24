package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID)
public final class FireArrowEvents {
    private FireArrowEvents() {
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!arrow.getPersistentData().getBoolean(FirePatchManager.TAG)) {
            return;
        }
        if (!(arrow.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = event.getRayTraceResult() == null ? arrow.position() : event.getRayTraceResult().getLocation();
        try {
            FirePatchManager.spawn(level, at, 120, 2.5);
        } catch (RuntimeException exception) {
            Gesturalcombat.LOGGER.error("Failed to spawn fire patch", exception);
        }
        arrow.discard();
        event.setCanceled(true);
    }
}
