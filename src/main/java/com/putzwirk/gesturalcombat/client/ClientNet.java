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

    public static void abilitySync(String abilities, String weapons) {
        com.putzwirk.gesturalcombat.technique.AbilityConfigs.loadFromJson(abilities, weapons);
    }

    public static void mastery(CompoundTag data) {
        ClientState.applyMastery(data);
    }

    public static void castEffect(int casterId, String techniqueId, int cooldownTicks, int swipe, boolean offhand, String cooldownKey) {
        Technique technique = Techniques.get(techniqueId);
        if (technique == null) {
            return;
        }
        CameraEffects.pulse(technique.archetype());
        playCastAnimation(casterId, technique, swipe, offhand);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == casterId) {
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

    private static void playCastAnimation(int casterId, Technique technique, int swipe, boolean offhand) {
        Minecraft mc = Minecraft.getInstance();
        Player caster = casterOf(mc, casterId);
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = caster != null ? caster.getItemInHand(hand) : ItemStack.EMPTY;
        Archetype archetype = technique.archetype();
        int swings = Math.max(1, AbilityConfigs.hits(technique));
        int interval = AbilityConfigs.hitInterval(technique);
        String[] sequence = AnimationCatalog.sequenceFor(archetype, technique.shape(), stack, swipe, swings);
        if (sequence == null) {
            return;
        }
        float baseLength = AnimationCatalog.swingLength(archetype, stack);
        float fastLength = AnimationCatalog.swingLength(interval);
        int fixed = AnimationCatalog.fixedLength(technique.shape());
        float[] lengths = new float[sequence.length];
        float[] upswings = new float[sequence.length];
        for (int i = 0; i < sequence.length; i++) {
            lengths[i] = fixed > 0 ? fixed : interval > 0 ? fastLength : baseLength;
            upswings[i] = fixed > 0 ? 0.25f : interval > 0 ? 0.5f : 0.2f;
        }
        String handName = offhand ? "OFF_HAND" : BetterCombatBridge.handName(stack);
        int windup = AbilityConfigs.windupTicks(technique);
        AnimationScheduler.playSequence(casterId, sequence, handName, lengths, upswings, windup, interval);
        if (archetype == Archetype.SWORD || archetype == Archetype.AXE || archetype == Archetype.SPEAR) {
            int life = windup + (int) Math.ceil(Math.max(lengths[lengths.length - 1], (swings - 1) * interval + lengths[0])) + 20;
            com.putzwirk.gesturalcombat.client.fx.TrailManager.start(casterId, life, 0xFFFFFF, offhand);
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
