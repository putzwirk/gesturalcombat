package com.putzwirk.gesturalcombat.server;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.HandGate;
import com.putzwirk.gesturalcombat.core.GestureShape;
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
import com.putzwirk.gesturalcombat.technique.OutLock;
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
        if (OutLock.isLocked(player, offhand)) {
            return;
        }
        if (com.putzwirk.gesturalcombat.weapon.WeaponRules.blocked(player.getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND), shape)) {
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
        Technique resolved = Techniques.forGesture(active, shape, stack);
        if (resolved == null || resolved.archetype() != active) {
            return;
        }
        technique = resolved;
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
        boolean twin = special == null
                && shape != GestureShape.V
                && com.putzwirk.gesturalcombat.weapon.DualWield.paired(player)
                && com.putzwirk.gesturalcombat.weapon.DualWield.link(player, offhand, now);
        ModNetwork.sendToTracking(player, new CastEffectPacket(player.getId(), technique.id(), cooldown, swipe, offhand, cooldownKey));
        String family = WeaponProfiles.family(stack);
        if (family == null || family.isBlank()) {
            family = com.putzwirk.gesturalcombat.compat.BetterCombatBridge.category(stack);
        }
        if (Config.INSTANCE.debugCasts.get()) {
            com.putzwirk.gesturalcombat.Gesturalcombat.LOGGER.info("gesturalcombat cast: {} family={} shape={} hand={} special={}",
                    technique.id(), family, shape, offhand ? "off" : "main", special != null);
        }
        boolean launched = false;
        if (special == null && level instanceof ServerLevel serverLevel) {
            double damage = EffectSupport.damageOf(player, technique, stack)
                    * AbilityConfigs.damageMultiplier(technique, stack, 1.0);
            if ("chakram".equalsIgnoreCase(family) && shape == GestureShape.CIRCLE) {
                launched = ThrownChakramEntity.launchChain(serverLevel, player, stack, technique, damage, offhand);
            } else if ("chakram".equalsIgnoreCase(family) && shape == GestureShape.ZIGZAG) {
                launched = ThrownChakramEntity.launchSaw(serverLevel, player, stack, technique, damage, offhand);
            } else if ("chakram".equalsIgnoreCase(family) && shape == GestureShape.LINE_H) {
                ThrownChakramEntity.launch(serverLevel, player, stack, technique, damage, EffectSupport.weaponReach(stack), offhand);
                launched = true;
            }
        }
        final Technique cast = technique;
        final WeaponSpecial applied = special;
        final boolean thrown = launched;
        final boolean sound = cast.shape() != GestureShape.V
                && cast.shape() != GestureShape.CARET
                && cast.archetype() != Archetype.SHIELD
                && cast.archetype() != Archetype.BOW;
        Runnable resolve = () -> {
            if (!player.isAlive() || player.isRemoved()) {
                return;
            }
            if (applied != null) {
                WeaponSpecialEffect.execute(player, level, stack, cast, applied);
            } else if (!thrown) {
                cast.effect().execute(player, level, stack, cast);
            }
            if (sound) {
                EffectSupport.swingSound(level, player, cast);
            }
        };
        int windup = AbilityConfigs.windupTicks(cast);
        int interval = AbilityConfigs.hitInterval(cast);
        int lead = windup > 0 ? windup : Math.max(2, interval);
        boolean diving = special == null && !thrown
                && cast.shape() == GestureShape.LINE_V_DOWN
                && com.putzwirk.gesturalcombat.weapon.WeaponRules.heavy(stack)
                && AerialSlam.begin(player, () -> {
                    resolve.run();
                    if (sound) {
                        swingSounds(level, player, cast, stack, 0, interval);
                    }
                });
        if (!diving) {
            com.putzwirk.gesturalcombat.technique.GestureScheduler.schedule(level, lead, resolve);
            if (sound) {
                swingSounds(level, player, cast, stack, lead, interval);
            }
        }
        if (twin) {
            int echoDelay = lead + 3;
            com.putzwirk.gesturalcombat.technique.GestureScheduler.schedule(level, echoDelay,
                    () -> com.putzwirk.gesturalcombat.server.TwinEcho.strike(player, level, offhand, cast, swipe));
        }
        EquipmentSlot slot = offhand ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(slot));
        player.causeFoodExhaustion(0.3f);
        player.resetAttackStrengthTicker();
    }

    private static void swingSounds(Level level, ServerPlayer player, Technique cast, ItemStack stack,
                                    int base, int interval) {
        int swings = Math.max(1, AbilityConfigs.hits(cast));
        String swingFamily = WeaponProfiles.family(stack);
        if ("greathammer".equalsIgnoreCase(swingFamily)
                && cast.shape() != GestureShape.LINE_H
                && cast.shape() != GestureShape.ZIGZAG) {
            swings = 1;
        }
        int[] ramps = AbilityConfigs.hitDelays(cast, stack);
        for (int i = 1; i < swings; i++) {
            int delay = base + (i < ramps.length ? ramps[i] : i * Math.max(1, interval));
            com.putzwirk.gesturalcombat.technique.GestureScheduler.schedule(level, delay, () -> {
                if (player.isAlive() && !player.isRemoved()) {
                    EffectSupport.swingSound(level, player, cast);
                }
            });
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
