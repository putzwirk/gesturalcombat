package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.core.Archetype;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class ShieldEvents {
    private ShieldEvents() {
    }

        public static boolean allowDamage(Player player, DamageSource damage) {
        if (player.level().isClientSide()) {
            return true;
        }
        if (TechniqueImmunity.isImmune(player)) {
            return damage.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }
        if (!GuardState.isGuarding(player)) {
            return true;
        }
        Entity source = damage.getDirectEntity();
        if (source == null) {
            source = damage.getEntity();
        }
        if (source == null) {
            return true;
        }
        Vec3 toSource = source.position().subtract(player.position());
        if (toSource.lengthSqr() < 1.0e-4) {
            return true;
        }
        Vec3 look = player.getLookAngle();
        return look.dot(toSource.normalize()) < 0.0;
    }

        public static boolean allowUse(Player player, ItemStack stack) {
        if (!Archetype.isShield(stack)) {
            return true;
        }
        return !Config.INSTANCE.gesturesEnabled.get() || GuardState.isGuarding(player);
    }
}
