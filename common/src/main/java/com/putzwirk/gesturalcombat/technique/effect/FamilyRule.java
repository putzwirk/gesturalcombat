package com.putzwirk.gesturalcombat.technique.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface FamilyRule {
    double adjust(ServerPlayer player, LivingEntity target, double damage, int index);
}
