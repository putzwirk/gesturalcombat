package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.core.Archetype;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID)
public final class ShieldEvents {
    private ShieldEvents() {
    }

    @SubscribeEvent
    public static void guarding(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (TechniqueImmunity.isImmune(player)) {
            if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
            }
            return;
        }
        if (!GuardState.isGuarding(player)) {
            return;
        }
        Entity source = event.getSource().getDirectEntity();
        if (source == null) {
            source = event.getSource().getEntity();
        }
        if (source == null) {
            return;
        }
        Vec3 toSource = source.position().subtract(player.position());
        if (toSource.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 look = player.getLookAngle();
        if (look.dot(toSource.normalize()) >= 0.0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void startUse(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!Archetype.isShield(event.getItem())) {
            return;
        }
        if (Config.INSTANCE.gesturesEnabled.get() && !GuardState.isGuarding(player)) {
            event.setCanceled(true);
        }
    }
}
