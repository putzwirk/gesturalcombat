package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.client.anim.AnimationCatalog;
import com.putzwirk.gesturalcombat.client.anim.AnimationScheduler;
import com.putzwirk.gesturalcombat.client.anim.Animations;
import com.putzwirk.gesturalcombat.client.anim.CameraEffects;
import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.GuardState;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ClientNet {
    private ClientNet() {
    }

    public static void abilitySync(String abilities, String weapons, String audit) {
        boolean empty = (abilities == null || abilities.isBlank()) && (weapons == null || weapons.isBlank());
        if (!empty) {
            com.putzwirk.gesturalcombat.technique.AbilityConfigs.loadFromJson(abilities, weapons);
            com.putzwirk.gesturalcombat.technique.AbilityConfigs.markSynced();
        }
        com.putzwirk.gesturalcombat.Gesturalcombat.LOGGER.info(
                "gesturalcombat client sync: {} techniques, {} profiles, {} player animations (payload {} chars / {} chars)",
                com.putzwirk.gesturalcombat.technique.AbilityConfigs.count(),
                com.putzwirk.gesturalcombat.weapon.WeaponProfiles.count(),
                BetterCombatBridge.animationCount("gesturalcombat"),
                abilities == null ? -1 : abilities.length(), weapons == null ? -1 : weapons.length());
        com.putzwirk.gesturalcombat.weapon.Parity.reportDrift();
        com.putzwirk.gesturalcombat.client.WeaponAuditClient.run(audit);
    }

    public static void castEffect(int casterId, String techniqueId, int cooldownTicks, int swipe, boolean offhand, String cooldownKey) {
        Technique technique = Techniques.get(techniqueId);
        if (technique == null) {
            return;
        }
        playCastAnimation(casterId, technique, swipe, offhand);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == casterId) {
            CameraEffects.pulse(technique.archetype(),
                    com.putzwirk.gesturalcombat.weapon.WeaponRules.shakeScale(technique.family()));
            com.putzwirk.gesturalcombat.client.render.HitboxDebug.capture(technique,
                    offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            ClientState.lastTechnique = techniqueId;
            ClientState.flashTicks = 14;
            ClientState.strokeStrength = 0;
            if (!"shield_guard".equals(techniqueId) && ClientState.guardTicks > 0) {
                ClientState.guardTicks = 0;
                GuardState.end(mc.player);
                if (mc.player.isUsingItem()) {
                    mc.player.stopUsingItem();
                }
            }
            if ("shield_guard".equals(techniqueId)) {
                ClientState.guardTicks = 40;
                GuardState.begin(mc.player, 40);
                mc.player.startUsingItem(InteractionHand.OFF_HAND);
            } else if ("shield_throw".equals(techniqueId)) {
                ClientState.shieldLockTicks = 100;
            }
            String key = cooldownKey == null || cooldownKey.isBlank() ? techniqueId : cooldownKey;
            ClientState.startCooldown(key, Math.max(4, cooldownTicks));
            if (mc.level != null) {
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.25f, 1.6f, false);
            }
        }
    }

    public static void twinCast(int casterId, String techniqueId, int swipe, boolean offhand) {
        Technique technique = Techniques.get(techniqueId);
        if (technique == null) {
            return;
        }
        playCastAnimation(casterId, technique, swipe, offhand, true);
    }

    private static void playCastAnimation(int casterId, Technique technique, int swipe, boolean offhand) {
        playCastAnimation(casterId, technique, swipe, offhand, false);
    }

    private static void playCastAnimation(int casterId, Technique technique, int swipe, boolean offhand, boolean echo) {
        Minecraft mc = Minecraft.getInstance();
        Player caster = casterOf(mc, casterId);
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = caster != null ? caster.getItemInHand(hand) : ItemStack.EMPTY;
        Archetype archetype = technique.archetype();
        String family = com.putzwirk.gesturalcombat.weapon.WeaponProfiles.family(stack);
        int swings = AnimationCatalog.swingCount(technique, family);
        int interval = AbilityConfigs.hitInterval(technique);
        boolean pairedGuard = technique.shape() == com.putzwirk.gesturalcombat.core.GestureShape.V
                && caster != null
                && com.putzwirk.gesturalcombat.weapon.DualWield.paired(caster.getMainHandItem(), caster.getOffhandItem());
        String[] sequence = AnimationCatalog.sequenceFor(archetype, technique.shape(), stack, swipe, swings, pairedGuard);
        if (sequence == null) {
            return;
        }
        if (echo && sequence.length > 1) {
            for (int i = 0; i < sequence.length / 2; i++) {
                String swap = sequence[i];
                sequence[i] = sequence[sequence.length - 1 - i];
                sequence[sequence.length - 1 - i] = swap;
            }
        }
        float baseLength = AnimationCatalog.swingLength(archetype, stack);
        int fixed = AnimationCatalog.fixedLength(technique.shape());
        int[] ramps = AbilityConfigs.hitDelays(technique, stack);
        int[] swingDelays = new int[sequence.length];
        float[] lengths = new float[sequence.length];
        float[] upswings = new float[sequence.length];
        String handName = offhand ? "OFF_HAND" : BetterCombatBridge.handName(stack);
        int windup = AbilityConfigs.windupTicks(technique);
        for (int i = 0; i < sequence.length; i++) {
            int step = i + 1 < ramps.length ? ramps[i + 1] - ramps[i] : interval;
            swingDelays[i] = i < ramps.length ? ramps[i] : ramps.length > 0 ? ramps[ramps.length - 1] : i * interval;
            lengths[i] = fixed > 0 ? fixed : step > 0 ? AnimationCatalog.swingLength(step) : baseLength;
            upswings[i] = fixed > 0 ? 0.25f : step > 0 ? 0.5f : 0.2f;
            if (windup > 0) {
                if (step > 0) {
                    lengths[i] = Math.max(lengths[i], windup + step + 2);
                    upswings[i] = 0.45f;
                } else {
                    lengths[i] = lengths[i] + windup;
                    upswings[i] = Math.min(0.60f, Math.max(0.28f, (windup + 0.5f) / lengths[i]));
                }
            }
        }
        AnimationScheduler.playSequence(casterId, sequence, handName, lengths, upswings, 0, swingDelays);
        if (archetype == Archetype.SWORD || archetype == Archetype.AXE || archetype == Archetype.SPEAR) {
            int last = swingDelays.length > 0 ? swingDelays[swingDelays.length - 1] : 0;
            int life = windup + last + (int) Math.ceil(lengths[0]) + 6;
            com.putzwirk.gesturalcombat.client.fx.TrailManager.start(casterId, life, windup,
                    echo ? 0xDCD2FF : 0xFFFFFF, offhand);
        }
    }

    private static Player casterOf(Minecraft mc, int casterId) {
        if (mc.player != null && mc.player.getId() == casterId) {
            return mc.player;
        }
        if (mc.level == null) {
            return null;
        }
        Entity entity = mc.level.getEntity(casterId);
        return entity instanceof Player player ? player : null;
    }

    public static void salto(int casterId, int ticks) {
        Player caster = casterOf(Minecraft.getInstance(), casterId);
        if (caster == null) {
            return;
        }
        String hand = BetterCombatBridge.handName(caster.getMainHandItem());
        com.putzwirk.gesturalcombat.client.fx.SaltoRotation.begin(casterId, ticks);
        AnimationScheduler.playSequence(casterId, new String[]{"gesturalcombat:salto_back"}, hand,
                new float[]{Math.max(6, ticks)}, new float[]{0.35f}, 0, new int[]{0});
    }

    public static void animation(int playerId, String animationId, String handName, boolean stop) {
        if (stop) {
            AnimationScheduler.cancel(playerId);
            Animations.stopFor(playerId);
        } else {
            Animations.playFor(playerId, animationId, handName, 14.0f, 0.4f);
        }
    }

    public static void reject() {
        ClientState.rejectTicks = 12;
        ClientState.TRAIL.clear();
    }
}
