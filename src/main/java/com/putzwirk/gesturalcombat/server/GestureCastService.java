package com.putzwirk.gesturalcombat.server;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.HandGate;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.mastery.MasteryCapability;
import com.putzwirk.gesturalcombat.mastery.MasteryService;
import com.putzwirk.gesturalcombat.network.CastEffectPacket;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.CastContext;
import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.GuardState;
import com.putzwirk.gesturalcombat.technique.ShieldLock;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import com.putzwirk.gesturalcombat.entity.ThrownChakramEntity;
import com.putzwirk.gesturalcombat.technique.effect.WeaponSpecialEffect;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import com.putzwirk.gesturalcombat.weapon.WeaponSpecial;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GestureCastService {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    private GestureCastService() {
    }

    public static void cast(ServerPlayer player, String techniqueId, GestureShape shape, float confidence, int swipe, boolean offhand) {
        if (!Config.INSTANCE.gesturesEnabled.get()) {
            return;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Technique technique = Techniques.get(techniqueId);
        if (technique == null) {
            return;
        }
        if (confidence < 0.4f) {
            return;
        }
        if (shape == null || shape == GestureShape.UNKNOWN || technique.shape() != shape) {
            return;
        }
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (offhand && HandGate.blocksOffhand(player.getMainHandItem())) {
            return;
        }
        if (GuardState.isGuarding(player)) {
            GuardState.end(player);
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
        }
        Archetype active = Archetype.forHand(player, hand);
        if (active == null || active != technique.archetype()) {
            return;
        }
        if (active == Archetype.SHIELD && ShieldLock.isLocked(player)) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }
        if (!MasteryCapability.get(player).isTechniqueAvailable(technique)) {
            return;
        }
        CastContext.swipe(player, swipe);
        WeaponSpecial special = WeaponProfiles.special(stack, shape);
        String cooldownKey = com.putzwirk.gesturalcombat.weapon.CooldownKeys.of(stack, shape, technique);
        long now = level.getGameTime();
        pruneCooldowns(player);
        Map<String, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Long end = playerCooldowns.get(cooldownKey);
        if (end != null && now < end) {
            return;
        }
        int cooldown = special == null
                ? EffectSupport.cooldownOf(player, technique, stack)
                : EffectSupport.specialCooldown(player, technique, stack, special.cooldownTicks());
        playerCooldowns.put(cooldownKey, now + cooldown);
        ModNetwork.sendToTracking(player, new CastEffectPacket(player.getId(), technique.id(), cooldown, swipe, offhand, cooldownKey));
        String category = com.putzwirk.gesturalcombat.compat.BetterCombatBridge.category(stack);
        boolean chakramThrow = special == null
                && "chakram".equalsIgnoreCase(category)
                && (shape == GestureShape.LINE_H || shape == GestureShape.LINE_V_DOWN)
                && level instanceof ServerLevel;
        if (special != null) {
            WeaponSpecialEffect.execute(player, level, stack, technique, special);
        } else if (chakramThrow) {
            ThrownChakramEntity.launch((ServerLevel) level, player, stack, technique,
                    EffectSupport.damageOf(player, technique, stack)
                            * AbilityConfigs.damageMultiplier(technique, stack, 1.0),
                    EffectSupport.weaponReach(stack));
        } else {
            technique.effect().execute(player, level, stack, technique);
        }
        MasteryService.award(player, technique.archetype(), technique.xp());
        EquipmentSlot slot = offhand ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(slot));
        player.causeFoodExhaustion(0.3f);
        player.resetAttackStrengthTicker();
        if (AbilityConfigs.hits(technique) <= 1
                && technique.shape() != GestureShape.V
                && technique.shape() != GestureShape.CARET
                && technique.archetype() != Archetype.SHIELD
                && technique.archetype() != Archetype.BOW) {
            EffectSupport.swingSound(level, player, technique);
        }
    }

    private static void pruneCooldowns(ServerPlayer player) {
        if (COOLDOWNS.size() <= 256) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server != null) {
            COOLDOWNS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        }
        if (COOLDOWNS.size() > 256) {
            COOLDOWNS.clear();
        }
    }
}
