package com.putzwirk.gesturalcombat.technique.effect;

import com.putzwirk.gesturalcombat.technique.EffectSupport;
import com.putzwirk.gesturalcombat.technique.Fx;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.putzwirk.gesturalcombat.weapon.WeaponSpecial;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

public final class WeaponSpecialEffect {
    private WeaponSpecialEffect() {
    }

    public static void execute(ServerPlayer player, Level level, ItemStack stack, Technique technique, WeaponSpecial special) {
        Hitbox hitbox = EffectSupport.scaleHitbox(player, technique, stack, special.hitbox());
        float damage = (float) (EffectSupport.damageOf(player, technique, stack) * special.damageMultiplier());
        for (LivingEntity target : hitbox.query(player, level)) {
            EffectSupport.hit(player, target, damage, technique, special.knockback(), special.lift(), true);
            for (WeaponSpecial.EffectSpec spec : special.targetEffects()) {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(spec.id()));
                if (effect != null) {
                    target.addEffect(new MobEffectInstance(effect, spec.duration(), spec.amplifier()));
                }
            }
        }
        for (WeaponSpecial.EffectSpec spec : special.selfEffects()) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(spec.id()));
            if (effect != null) {
                player.addEffect(new MobEffectInstance(effect, spec.duration(), spec.amplifier()));
            }
        }
        if (special.heal() > 0.0) {
            player.heal((float) special.heal());
        }
        Fx.special(player, technique, special, hitbox);
        ParticleOptions accent = particles(special.particles());
        if (accent != null) {
            EffectSupport.particles(level, accent, player.getX(), player.getY() + 1.0, player.getZ(),
                    12, hitbox.range() * 0.3, 0.5, hitbox.range() * 0.3, 0.02);
        }
        SoundEvent sound = special.sound().isBlank() ? null : BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(special.sound()));
        if (sound != null) {
            EffectSupport.sound(level, player, sound, 1.0f, 1.0f);
        }
    }

    private static ParticleOptions particles(String name) {
        return switch (name == null ? "" : name) {
            case "flame" -> ParticleTypes.FLAME;
            case "soul" -> ParticleTypes.SOUL;
            case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "crit" -> ParticleTypes.CRIT;
            case "explosion" -> ParticleTypes.EXPLOSION;
            case "electric_spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "snowflake" -> ParticleTypes.SNOWFLAKE;
            case "enchanted_hit" -> ParticleTypes.ENCHANTED_HIT;
            case "witch" -> ParticleTypes.WITCH;
            case "end_rod" -> ParticleTypes.END_ROD;
            default -> null;
        };
    }
}
