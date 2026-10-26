package com.putzwirk.gesturalcombat.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface ArrowPierceInvoker {
    @Invoker("setPierceLevel")
    void gesturalcombat$setPierce(byte level);
}
