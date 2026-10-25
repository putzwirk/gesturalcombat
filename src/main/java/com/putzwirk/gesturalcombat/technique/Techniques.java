package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.effect.ArcSweepEffect;
import com.putzwirk.gesturalcombat.technique.effect.BleedEffect;
import com.putzwirk.gesturalcombat.technique.effect.BraceEffect;
import com.putzwirk.gesturalcombat.technique.effect.BowFireEffect;
import com.putzwirk.gesturalcombat.technique.effect.FlurryEffect;
import com.putzwirk.gesturalcombat.technique.effect.GlaiveSweepEffect;
import com.putzwirk.gesturalcombat.technique.effect.GroundSlamEffect;
import com.putzwirk.gesturalcombat.technique.effect.CrossSlashEffect;
import com.putzwirk.gesturalcombat.technique.effect.DashStrikeEffect;
import com.putzwirk.gesturalcombat.technique.effect.DeathblowEffect;
import com.putzwirk.gesturalcombat.technique.effect.EdgePairedEffect;
import com.putzwirk.gesturalcombat.technique.effect.FamilyArcEffect;
import com.putzwirk.gesturalcombat.technique.effect.FamilyFlurryEffect;
import com.putzwirk.gesturalcombat.technique.effect.GuardEffect;
import com.putzwirk.gesturalcombat.technique.effect.LinePierceEffect;
import com.putzwirk.gesturalcombat.technique.effect.SlamPierceEffect;
import com.putzwirk.gesturalcombat.technique.effect.MaceSlamEffect;
import com.putzwirk.gesturalcombat.technique.effect.MomentumSweepEffect;
import com.putzwirk.gesturalcombat.technique.effect.PostureEffect;
import com.putzwirk.gesturalcombat.technique.effect.PostureFlurryEffect;
import com.putzwirk.gesturalcombat.technique.effect.RampFlurryEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldBashEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldChargeEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldGuardEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldThrowEffect;
import com.putzwirk.gesturalcombat.technique.effect.SpinEffect;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Techniques {
    private static final Map<String, Technique> BY_ID = new LinkedHashMap<>();
    private static final Map<Archetype, Map<GestureShape, Technique>> BY_ARCHETYPE = new EnumMap<>(Archetype.class);
    private static final Map<String, Map<GestureShape, Technique>> BY_FAMILY = new LinkedHashMap<>();

    public static final Technique SWORD_SLASH = add(new Technique("sword_slash", Archetype.SWORD, GestureShape.LINE_H, 1, 10, null, 6, new ArcSweepEffect(2.8, 110, 1.0, 0.0, 0.25)));
    public static final Technique SWORD_RISING = add(new Technique("sword_rising", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 18, "sword_t1", 8, new ArcSweepEffect(2.8, 120, 1.10, 0.55, 0.0)));
    public static final Technique SWORD_WHIRL = add(new Technique("sword_whirl", Archetype.SWORD, GestureShape.CIRCLE, 4, 30, "sword_t2", 12, new SpinEffect(3.1, 1.35, 2, 8)));
    public static final Technique SWORD_FLURRY = add(new Technique("sword_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 7, 44, "sword_t3", 14, new FlurryEffect(2.9, 90, 1.65, 3, 5)));
    public static final Technique SWORD_EXECUTE = add(new Technique("sword_execute", Archetype.SWORD, GestureShape.LINE_V_DOWN, 10, 50, "sword_t4", 20, new ArcSweepEffect(2.6, 70, 1.95, 0.0, 0.5)));
    public static final Technique SWORD_GUARD = add(new Technique("sword_guard", Archetype.SWORD, GestureShape.V, 1, 200, null, 10, new GuardEffect(20, 2)));
    public static final Technique SWORD_SKIRMISH = add(new Technique("sword_skirmish", Archetype.SWORD, GestureShape.CARET, 1, 60, null, 10,
            new DashStrikeEffect(3.6, 120, 1.45, 4.6, 0.0, 2, (player, target, damage, index) -> damage, 2, true)));

    public static final Technique KATANA_SLASH = add(new Technique("katana_slash", Archetype.SWORD, GestureShape.LINE_H, 1, 12, null, 7, new PostureEffect(3.1, 100, 0.9, 18), "katana"));
    public static final Technique KATANA_RISING = add(new Technique("katana_rising", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 16, null, 8, new PostureEffect(2.6, 120, 1.15, 15, 0.4), "katana"));
    public static final Technique KATANA_WHIRL = add(new Technique("katana_whirl", Archetype.SWORD, GestureShape.CIRCLE, 4, 26, null, 12, new PostureEffect(3.1, 150, 1.3, 25), "katana"));
    public static final Technique KATANA_FLURRY = add(new Technique("katana_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 7, 44, null, 14, new PostureFlurryEffect(3.1, 90, 2.0, 3, 5, 9), "katana"));
    public static final Technique KATANA_EXECUTE = add(new Technique("katana_execute", Archetype.SWORD, GestureShape.LINE_V_DOWN, 10, 50, null, 20, new DeathblowEffect(2.6, 110, 2.4), "katana"));
    public static final Technique KATANA_STEP = add(new Technique("katana_step", Archetype.SWORD, GestureShape.CARET, 1, 30, null, 9,
            new DashStrikeEffect(3.0, 130, 0.85, 5.0, 0.0, 3, (player, target, damage, index) -> {
                States.stagger(target, 20);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 0, false, false, false));
                return damage;
            }), "katana"));

    public static final Technique GLAIVE_SWEEP = add(new Technique("glaive_sweep", Archetype.SPEAR, GestureShape.LINE_H, 1, 16, "spear_t1", 8, new GlaiveSweepEffect(3.9, 165, 1.10), "glaive"));
    public static final Technique GLAIVE_RISE = add(new Technique("glaive_rise", Archetype.SPEAR, GestureShape.LINE_V_UP, 1, 20, null, 9,
            new FamilyArcEffect(3.2, 130, 1.30, 0.0, 0.5, (player, target, damage, index) -> {
                States.stagger(target, 25);
                return damage;
            }), "glaive"));
    public static final Technique GLAIVE_DROP = add(new Technique("glaive_drop", Archetype.SPEAR, GestureShape.LINE_V_DOWN, 7, 44, null, 16,
            new FamilyArcEffect(2.8, 90, 2.15, 0.7, 0.0, (player, target, damage, index) -> {
                double bonus = States.staggered(target) ? 1.35 : 1.0;
                States.stagger(target, 25);
                HitStun.pin(target, 8);
                return damage * bonus;
            }), "glaive"));
    public static final Technique GLAIVE_Z = add(new Technique("glaive_z", Archetype.SPEAR, GestureShape.ZIGZAG, 7, 38, null, 14, new GlaiveSweepEffect(3.9, 220, 1.72), "glaive"));
    public static final Technique TRIDENT_THRUST = add(new Technique("trident_thrust", Archetype.SPEAR, GestureShape.LINE_H, 1, 16, null, 6,
            new LinePierceEffect(3.9, 0.6, 0.68, 0.0, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "trident"));
    public static final Technique TRIDENT_RISE = add(new Technique("trident_rise", Archetype.SPEAR, GestureShape.LINE_V_UP, 1, 22, null, 8,
            new FamilyArcEffect(4.0, 55, 0.78, 0.0, 0.3, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "trident"));
    public static final Technique TRIDENT_DROP = add(new Technique("trident_drop", Archetype.SPEAR, GestureShape.LINE_V_DOWN, 7, 46, null, 15,
            new FamilyArcEffect(3.5, 50, 1.2, 0.0, 0.0, (player, target, damage, index) -> {
                int bleed = States.bleed(target);
                States.addBleed(target, player, 2);
                HitStun.pin(target, 8);
                return damage * (bleed >= 2 ? 1.4 : 1.0);
            }), "trident"));
    public static final Technique TRIDENT_WHIRL = add(new Technique("trident_whirl", Archetype.SPEAR, GestureShape.CIRCLE, 4, 38, null, 13,
            new FamilyFlurryEffect(3.6, 300, 0.72, 2, 5, 0.2, 0.0, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "trident"));
    public static final Technique TRIDENT_Z = add(new Technique("trident_z", Archetype.SPEAR, GestureShape.ZIGZAG, 4, 32, null, 10,
            new FamilyFlurryEffect(4.0, 45, 0.95, 3, 3, 0.2, 0.0, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "trident"));
    public static final Technique TRIDENT_SURGE = add(new Technique("trident_surge", Archetype.SPEAR, GestureShape.CARET, 1, 30, null, 9,
            new DashStrikeEffect(3.8, 80, 0.95, 5.6, 0.0, 3, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "trident"));
    public static final Technique TRIDENT_WARD = add(new Technique("trident_ward", Archetype.SPEAR, GestureShape.V, 1, 180, null, 10,
            new BraceEffect(10), "trident"));

    public static final Technique MACE_CUT = add(new Technique("mace_cut", Archetype.AXE, GestureShape.LINE_H, 1, 11, null, 8,
            new FamilyArcEffect(3.2, 160, 1.77, 0.50, 0.0, (player, target, damage, index) -> {
                States.stagger(target, 20);
                return damage;
            }), "mace"));
    public static final Technique MACE_RISE = add(new Technique("mace_rise", Archetype.AXE, GestureShape.LINE_V_UP, 1, 12, null, 10,
            new FamilyArcEffect(3.0, 120, 1.92, 0.20, 0.40, (player, target, damage, index) -> {
                States.stagger(target, 20);
                return damage;
            }), "mace"));
    public static final Technique MACE_DROP = add(new Technique("mace_drop", Archetype.AXE, GestureShape.LINE_V_DOWN, 7, 50, null, 16,
            new MaceSlamEffect(2.9, 90, 2.70), "mace"));
    public static final Technique MACE_Z = add(new Technique("mace_z", Archetype.AXE, GestureShape.ZIGZAG, 4, 36, null, 12,
            new FamilyArcEffect(3.2, 210, 2.20, 0.60, 0.0, (player, target, damage, index) -> {
                States.stagger(target, 20);
                return damage;
            }), "mace"));
    public static final Technique GREATAXE_SWEEP = add(new Technique("greataxe_sweep", Archetype.AXE, GestureShape.LINE_H, 1, 20, null, 8,
            new FamilyArcEffect(4.0, 200, 1.62, 0.35, 0.0, (player, target, damage, index) -> {
                States.addDomain(player, 0);
                return damage;
            }), "greataxe"));
    public static final Technique GREATAXE_RISE = add(new Technique("greataxe_rise", Archetype.AXE, GestureShape.LINE_V_UP, 1, 24, null, 10,
            new FamilyArcEffect(3.6, 150, 2.13, 0.15, 0.55, (player, target, damage, index) -> {
                States.addDomain(player, 0);
                return damage;
            }), "greataxe"));
    public static final Technique GREATAXE_DROP = add(new Technique("greataxe_drop", Archetype.AXE, GestureShape.LINE_V_DOWN, 7, 52, null, 17,
            new FamilyArcEffect(3.2, 100, 3.50, 1.0, 0.0, (player, target, damage, index) -> {
                States.addDomain(player, 1);
                return damage;
            }), "greataxe"));
    public static final Technique GREATAXE_Z = add(new Technique("greataxe_z", Archetype.AXE, GestureShape.ZIGZAG, 7, 46, null, 16,
            new FamilyArcEffect(4.0, 300, 2.99, 0.80, 0.0, (player, target, damage, index) -> {
                States.addDomain(player, 0);
                return damage;
            }), "greataxe"));

    public static final Technique SWORD_LINK = add(new Technique("sword_link", Archetype.SWORD, GestureShape.LINE_H, 1, 12, null, 6,
            new CrossSlashEffect(3.1, 1.35, 0.85), "sword"));

    public static final Technique RAPIER_CUT = add(new Technique("rapier_thrust", Archetype.SWORD, GestureShape.LINE_H, 1, 11, null, 6,
            new LinePierceEffect(4.2, 0.55, 0.7, 0.12, (player, target, damage, index) -> {
                int seam = States.seam(target);
                if (seam >= 3) {
                    States.consumeSeam(target);
                    return damage * 2.2;
                }
                States.addSeam(target, 1);
                return damage;
            }), "rapier"));
    public static final Technique RAPIER_LUNGE = add(new Technique("rapier_lunge", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 16, null, 8,
            new LinePierceEffect(5.2, 0.55, 1.05, 0.5, (player, target, damage, index) -> {
                States.addSeam(target, 1);
                return damage;
            }), "rapier"));
    public static final Technique RAPIER_RIPOSTE = add(new Technique("rapier_riposte", Archetype.SWORD, GestureShape.CIRCLE, 4, 24, null, 12,
            new FamilyFlurryEffect(3.8, 140, 1.1, 2, 3, 0.0, 0.0, (player, target, damage, index) -> {
                boolean beat = index > 0 && States.seam(target) >= 2;
                States.addSeam(target, 1);
                return beat ? damage * 1.35 : damage;
            }), "rapier"));
    public static final Technique RAPIER_FLURRY = add(new Technique("rapier_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 4, 30, null, 12,
            new FamilyFlurryEffect(4.0, 30, 1.45, 5, 2, 0.2, 0.0, (player, target, damage, index) -> {
                States.addSeam(target, 1);
                return damage;
            }), "rapier"));
    public static final Technique RAPIER_DROP = add(new Technique("rapier_drop", Archetype.SWORD, GestureShape.LINE_V_DOWN, 7, 42, null, 15,
            new FamilyArcEffect(3.4, 40, 1.35, 0.2, 0.0, (player, target, damage, index) -> {
                if (States.seam(target) >= 2) {
                    States.consumeSeam(target);
                    return damage * 1.4;
                }
                return damage;
            }), "rapier"));
    public static final Technique RAPIER_FLECHE = add(new Technique("rapier_fleche", Archetype.SWORD, GestureShape.CARET, 1, 28, null, 9,
            new DashStrikeEffect(3.6, 45, 0.85, 6.2, 0.0, 2, (player, target, damage, index) -> {
                States.addSeam(target, 1);
                return damage;
            }), "rapier"));

    public static final Technique CLAYMORE_SWEEP = add(new Technique("claymore_sweep", Archetype.SWORD, GestureShape.LINE_H, 1, 18, null, 7,
            new FamilyArcEffect(4.2, 190, 1.56, 0.30, 0.0, (player, target, damage, index) -> {
                States.addSunder(target, 1);
                return damage;
            }), "claymore"));
    public static final Technique CLAYMORE_RISE = add(new Technique("claymore_rise", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 22, null, 9,
            new FamilyArcEffect(3.4, 140, 2.12, 0.15, 0.55, (player, target, damage, index) -> {
                States.addSunder(target, 1);
                return damage;
            }), "claymore"));
    public static final Technique CLAYMORE_Z = add(new Technique("claymore_z", Archetype.SWORD, GestureShape.ZIGZAG, 7, 48, null, 16,
            new FamilyArcEffect(4.2, 210, 2.98, 0.55, 0.0, (player, target, damage, index) -> {
                States.addSunder(target, 1);
                return damage;
            }), "claymore"));
    public static final Technique CLAYMORE_BREAK = add(new Technique("claymore_break", Archetype.SWORD, GestureShape.LINE_V_DOWN, 7, 52, null, 16,
            new SlamPierceEffect(3.2, 120, 6.0, 0.9, 0.56, 0.44, 0.55, (player, target, damage, index) -> {
                double bonus = States.sunder(target) >= 2 ? 1.5 : 1.0;
                States.addSunder(target, 1);
                return damage * bonus;
            }), "claymore"));
    public static final Technique SICKLE_CUT = add(new Technique("sickle_cut", Archetype.SWORD, GestureShape.LINE_H, 1, 9, null, 6,
            new FamilyArcEffect(2.8, 120, 0.9, 0.15, 0.0, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "sickle"));
    public static final Technique SICKLE_HARVEST = add(new Technique("sickle_hook", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 16, null, 8,
            new FamilyArcEffect(2.6, 110, 1.05, 0.0, 0.35, (player, target, damage, index) -> {
                States.refreshBleed(target, player, 1);
                return damage;
            }), "sickle"));
    public static final Technique SICKLE_FLURRY = add(new Technique("sickle_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 4, 32, null, 12,
            new FamilyFlurryEffect(2.8, 90, 1.75, 5, 3, 0.0, 0.0, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "sickle"));
    public static final Technique SICKLE_EVISCERATE = add(new Technique("sickle_eviscerate", Archetype.SWORD, GestureShape.LINE_V_DOWN, 7, 42, null, 15,
            new FamilyArcEffect(2.6, 90, 1.9, 0.0, 0.3, (player, target, damage, index) -> {
                return damage + States.consumeBleed(target);
            }), "sickle"));
    public static final Technique SICKLE_STEP = add(new Technique("sickle_step", Archetype.SWORD, GestureShape.CARET, 1, 28, null, 7,
            new DashStrikeEffect(2.8, 150, 1.10, 4.6, 0.0, 2, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }, 2, true), "sickle"));
    public static final Technique SICKLE_WHIRL = add(new Technique("sickle_whirl", Archetype.SWORD, GestureShape.CIRCLE, 4, 34, null, 12,
            new FamilyFlurryEffect(2.8, 360, 1.0, 2, 7, 0.2, 0.0, (player, target, damage, index) -> {
                States.addBleed(target, player, 1);
                return damage;
            }), "sickle"));

    public static final Technique DEBT_STAB = add(new Technique("saidebt_stab", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 13, null, 5,
            new FamilyArcEffect(2.6, 45, 0.95, 0.0, 0.0, (player, target, damage, index) -> {
                States.addDebt(player, 1);
                return Assassin.strike(player, target, damage, false);
            }), "soul_knife"));
    public static final Technique DEBT_SPIKE = add(new Technique("saidebt_spike", Archetype.SWORD, GestureShape.LINE_V_DOWN, 7, 40, null, 16,
            new FamilyArcEffect(2.6, 40, 1.9, 0.0, 0.0, (player, target, damage, index) -> {
                int debt = States.spendDebt(player);
                return Assassin.strike(player, target, damage + 1.2 * Math.floor(debt / 3.0), true);
            }), "soul_knife"));
    public static final Technique DEBT_TURN = add(new Technique("saidebt_turn", Archetype.SWORD, GestureShape.CIRCLE, 4, 22, null, 12,
            new FamilyFlurryEffect(2.4, 120, 1.2, 2, 3, 0.0, 0.0, (player, target, damage, index) -> {
                States.addDebt(player, 1);
                return Assassin.strike(player, target, damage, false);
            }), "soul_knife"));
    public static final Technique DEBT_FLURRY = add(new Technique("saidebt_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 4, 19, null, 10,
            new FamilyFlurryEffect(2.4, 45, 1.4, 4, 2, 0.0, 0.0, (player, target, damage, index) -> {
                States.addDebt(player, 1);
                return Assassin.strike(player, target, damage, false);
            }), "soul_knife"));
    public static final Technique DEBT_POKE = add(new Technique("saidebt_poke", Archetype.SWORD, GestureShape.LINE_H, 1, 12, null, 4,
            new FamilyArcEffect(2.7, 55, 0.9, 0.0, 0.0, (player, target, damage, index) -> {
                States.addDebt(player, 1);
                return Assassin.strike(player, target, damage, false);
            }), "soul_knife"));
    public static final Technique DEBT_STEP = add(new Technique("saidebt_step", Archetype.SWORD, GestureShape.CARET, 1, 26, null, 8,
            new DashStrikeEffect(2.6, 70, 1.10, 5.4, 0.0, 2, (player, target, damage, index) -> {
                States.addDebt(player, 1);
                return Assassin.strike(player, target, damage, false);
            }, 2, true), "soul_knife"));

    public static final Technique TWINBLADE_CUT = add(new Technique("twinblade_cut", Archetype.SWORD, GestureShape.LINE_H, 1, 14, null, 6,
            new FamilyFlurryEffect(2.6, 200, 0.88, 2, 4, 0.2, 0.0, (player, target, damage, index) -> {
                if (index == 0) {
                    States.addFlow(player, GestureShape.LINE_H);
                }
                return damage * (1.0 + 0.1 * States.flow(player));
            }), "battlestaff"));
    public static final Technique TWINBLADE_RISE = add(new Technique("twinblade_rise", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 20, null, 8,
            new FamilyFlurryEffect(2.6, 180, 1.08, 2, 4, 0.0, 0.3, (player, target, damage, index) -> {
                if (index == 0) {
                    States.addFlow(player, GestureShape.LINE_V_UP);
                }
                return damage * (1.0 + 0.1 * States.flow(player));
            }), "battlestaff"));
    public static final Technique TWINBLADE_FLURRY = add(new Technique("twinblade_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 4, 36, null, 12,
            new FamilyFlurryEffect(2.6, 360, 1.32, 4, 3, 0.4, 0.0, (player, target, damage, index) -> {
                if (index == 0) {
                    States.addFlow(player, GestureShape.ZIGZAG);
                }
                return damage * (1.0 + 0.1 * States.flow(player));
            }), "battlestaff"));
    public static final Technique TWINBLADE_SLAM = add(new Technique("twinblade_slam", Archetype.SWORD, GestureShape.LINE_V_DOWN, 7, 46, null, 15,
            new FamilyFlurryEffect(2.5, 140, 1.25, 3, 5, 0.5, 0.0, (player, target, damage, index) -> {
                if (index == 0) {
                    States.addFlow(player, GestureShape.LINE_V_DOWN);
                }
                return damage * (1.0 + 0.1 * States.flow(player));
            }), "battlestaff"));
    public static final Technique TWINBLADE_SPIN = add(new Technique("twinblade_spin", Archetype.SWORD, GestureShape.CIRCLE, 7, 42, null, 15, new SpinEffect(2.6, 1.12, 5, 6), "battlestaff"));
    public static final Technique TWINBLADE_STEP = add(new Technique("twinblade_step", Archetype.SWORD, GestureShape.CARET, 1, 28, null, 9,
            new DashStrikeEffect(2.8, 200, 1.05, 4.2, 0.0, 3, (player, target, damage, index) -> {
                States.addFlow(player, GestureShape.CARET);
                return damage * (1.0 + 0.1 * States.flow(player));
            }), "battlestaff"));
    public static final Technique WARGLAIVE_CUT = add(new Technique("warglaive_cut", Archetype.SPEAR, GestureShape.LINE_H, 1, 16, null, 7,
            new EdgePairedEffect(3.4, 0.35, 4, 1.25, 0.15), "warglaive"));
    public static final Technique WARGLAIVE_RISE = add(new Technique("warglaive_rise", Archetype.SPEAR, GestureShape.LINE_V_UP, 1, 22, null, 8,
            new FamilyFlurryEffect(3.4, 180, 0.98, 2, 4, 0.0, 0.25, (player, target, damage, index) ->
                    States.alternateEdge(player) ? damage * 1.25 : damage), "warglaive"));
    public static final Technique WARGLAIVE_FLURRY = add(new Technique("warglaive_flurry", Archetype.SPEAR, GestureShape.ZIGZAG, 4, 38, null, 12,
            new FamilyFlurryEffect(3.4, 360, 1.12, 4, 3, 0.4, 0.0, (player, target, damage, index) ->
                    States.alternateEdge(player) ? damage * 1.25 : damage), "warglaive"));
    public static final Technique WARGLAIVE_SLAM = add(new Technique("warglaive_slam", Archetype.SPEAR, GestureShape.LINE_V_DOWN, 7, 46, null, 15,
            new EdgePairedEffect(3.0, 0.51, 4, 1.25, 0.35), "warglaive"));
    public static final Technique WARGLAIVE_WHIRL = add(new Technique("warglaive_whirl", Archetype.SPEAR, GestureShape.CIRCLE, 7, 50, null, 16, new SpinEffect(3.3, 0.92, 3, 8), "warglaive"));
    public static final Technique WARGLAIVE_STEP = add(new Technique("warglaive_step", Archetype.SPEAR, GestureShape.CARET, 1, 30, null, 9,
            new DashStrikeEffect(3.2, 170, 1.10, 4.6, 0.0, 3, (player, target, damage, index) ->
                    States.alternateEdge(player) ? damage * 1.25 : damage), "warglaive"));
    public static final Technique HALBERD_CUT = add(new Technique("halberd_cut", Archetype.SPEAR, GestureShape.LINE_H, 1, 18, null, 8,
            new FamilyArcEffect(3.6, 150, 1.05, 0.15, 0.0, (player, target, damage, index) -> {
                States.stagger(target, 30);
                HitStun.pin(target, 5);
                return damage;
            }), "halberd"));
    public static final Technique HALBERD_RISE = add(new Technique("halberd_rise", Archetype.SPEAR, GestureShape.LINE_V_UP, 1, 22, null, 9,
            new FamilyArcEffect(3.2, 120, 1.32, 0.0, 0.6, (player, target, damage, index) -> {
                States.stagger(target, 30);
                HitStun.pin(target, 6);
                return damage;
            }), "halberd"));
    public static final Technique HALBERD_DROP = add(new Technique("halberd_drop", Archetype.SPEAR, GestureShape.LINE_V_DOWN, 7, 46, null, 16,
            new FamilyArcEffect(2.9, 70, 2.20, 0.3, 0.0, (player, target, damage, index) -> {
                double bonus = States.staggered(target) ? 1.35 : 1.0;
                States.stagger(target, 30);
                HitStun.pin(target, 10);
                return damage * bonus;
            }), "halberd"));
    public static final Technique HALBERD_FLURRY = add(new Technique("halberd_flurry", Archetype.SPEAR, GestureShape.ZIGZAG, 4, 44, null, 16,
            new FamilyArcEffect(3.4, 300, 2.35, 0.8, 0.0, (player, target, damage, index) -> {
                States.stagger(target, 40);
                HitStun.pin(target, 10);
                return damage;
            }), "halberd"));
    public static final Technique GREATHAMMER_CUT = add(new Technique("greathammer_cut", Archetype.AXE, GestureShape.LINE_H, 1, 22, null, 8,
            new FamilyArcEffect(4.0, 160, 2.78, 0.50, 0.0, (player, target, damage, index) -> {
                States.stagger(target, 25);
                HitStun.pin(target, 9);
                return damage;
            }), "greathammer"));
    public static final Technique GREATHAMMER_RISE = add(new Technique("greathammer_rise", Archetype.AXE, GestureShape.LINE_V_UP, 1, 25, null, 10,
            new FamilyArcEffect(3.4, 150, 3.07, 0.20, 0.45, (player, target, damage, index) -> {
                States.stagger(target, 25);
                HitStun.pin(target, 9);
                return damage;
            }), "greathammer"));
    public static final Technique GREATHAMMER_DROP = add(new Technique("greathammer_drop", Archetype.AXE, GestureShape.LINE_V_DOWN, 7, 60, null, 17,
            new FamilyArcEffect(3.0, 110, 4.24, 1.20, 0.0, (player, target, damage, index) -> {
                double bonus = States.staggered(target) ? 1.25 : 1.0;
                States.stagger(target, 25);
                HitStun.pin(target, 9);
                return damage * bonus;
            }), "greathammer"));
    public static final Technique GREATHAMMER_Z = add(new Technique("greathammer_z", Archetype.AXE, GestureShape.ZIGZAG, 7, 74, null, 20,
            new RampFlurryEffect(3.2, 170, 3.60, 5, 9, 4, 0.60, 0.0, (player, target, damage, index) -> {
                States.stagger(target, 40);
                HitStun.pin(target, index >= 3 ? 10 : 7);
                return damage;
            }), "greathammer"));
    public static final Technique SCYTHE_CUT = add(new Technique("scythe_cut", Archetype.AXE, GestureShape.LINE_H, 1, 18, null, 7,
            new FamilyArcEffect(3.0, 220, 0.90, 0.0, 0.0, (player, target, damage, index) -> {
                boolean bonus = States.harvestBonus(player);
                States.recordHarvest(player, target);
                if (bonus) {
                    player.heal(0.5f);
                    return damage * 1.15;
                }
                return damage;
            }), "scythe"));
    public static final Technique SCYTHE_RISE = add(new Technique("scythe_rise", Archetype.AXE, GestureShape.LINE_V_UP, 1, 24, null, 9,
            new FamilyArcEffect(2.8, 150, 1.12, 0.0, 0.45, (player, target, damage, index) -> {
                boolean bonus = States.harvestBonus(player);
                States.recordHarvest(player, target);
                return bonus ? damage * 1.15 : damage;
            }), "scythe"));
    public static final Technique SCYTHE_WHIRL = add(new Technique("scythe_whirl", Archetype.AXE, GestureShape.CIRCLE, 4, 48, null, 13,
            new FamilyFlurryEffect(3.0, 360, 1.05, 2, 8, 0.3, 0.0, (player, target, damage, index) -> {
                boolean bonus = States.harvestBonus(player);
                States.recordHarvest(player, target);
                if (bonus) {
                    player.heal(0.25f);
                    return damage * 1.15;
                }
                return damage;
            }), "scythe"));
    public static final Technique SCYTHE_Z = add(new Technique("scythe_z", Archetype.AXE, GestureShape.ZIGZAG, 7, 42, null, 15,
            new FamilyArcEffect(3.0, 360, 1.33, 0.3, 0.0, (player, target, damage, index) -> {
                boolean bonus = States.harvestBonus(player);
                States.recordHarvest(player, target);
                if (bonus) {
                    player.heal(0.5f);
                    return damage * 1.15;
                }
                return damage;
            }), "scythe"));
    public static final Technique SCYTHE_DROP = add(new Technique("scythe_drop", Archetype.AXE, GestureShape.LINE_V_DOWN, 7, 52, null, 16,
            new FamilyArcEffect(2.6, 120, 1.80, 0.4, 0.0, (player, target, damage, index) -> {
                boolean execute = target.getHealth() <= target.getMaxHealth() * 0.25f;
                States.recordHarvest(player, target);
                return damage * (execute ? 2.0 : 1.0);
            }), "scythe"));
    public static final Technique SCYTHE_STEP = add(new Technique("scythe_step", Archetype.AXE, GestureShape.CARET, 1, 32, null, 10,
            new DashStrikeEffect(3.0, 220, 0.95, 4.4, 0.0, 3, (player, target, damage, index) -> {
                boolean bonus = States.harvestBonus(player);
                States.recordHarvest(player, target);
                if (bonus) {
                    player.heal(0.25f);
                    return damage * 1.15;
                }
                return damage;
            }), "scythe"));
    public static final Technique PYRE_SWEEP = add(new Technique("pyre_sweep", Archetype.AXE, GestureShape.LINE_H, 1, 20, null, 8,
            new FamilyArcEffect(4.1, 180, 2.43, 0.30, 0.0, (player, target, damage, index) -> {
                if (target.getHealth() <= damage) {
                    States.addDomain(player, 1);
                }
                return damage;
            }), "heavy_axe"));
    public static final Technique PYRE_RISE = add(new Technique("pyre_rise", Archetype.AXE, GestureShape.LINE_V_UP, 1, 23, null, 9,
            new FamilyArcEffect(3.4, 140, 2.23, 0.15, 0.50, (player, target, damage, index) -> {
                if (target.getHealth() <= damage) {
                    States.addDomain(player, 1);
                }
                return damage;
            }), "heavy_axe"));
    public static final Technique PYRE_DROP = add(new Technique("pyre_drop", Archetype.AXE, GestureShape.LINE_V_DOWN, 7, 54, null, 16,
            new FamilyArcEffect(3.0, 95, 3.24, 1.0, 0.0, (player, target, damage, index) -> {
                if (target.getHealth() <= damage) {
                    States.addDomain(player, 2);
                }
                return damage;
            }), "heavy_axe"));
    public static final Technique PYRE_Z = add(new Technique("pyre_z", Archetype.AXE, GestureShape.ZIGZAG, 7, 46, null, 16,
            new FamilyArcEffect(4.1, 260, 3.14, 0.60, 0.0, (player, target, damage, index) -> {
                if (target.getHealth() <= damage) {
                    States.addDomain(player, 2);
                }
                return damage;
            }), "heavy_axe"));

    public static final Technique AXE_CLEAVE = add(new Technique("axe_cleave", Archetype.AXE, GestureShape.LINE_V_DOWN, 1, 12, null, 6, new ArcSweepEffect(2.9, 95, 1.15, 0.0, 0.35)));
    public static final Technique AXE_WIDE = add(new Technique("axe_wide", Archetype.AXE, GestureShape.LINE_H, 1, 20, "axe_t1", 9, new ArcSweepEffect(3.5, 175, 0.9, 0.0, 0.6)));
    public static final Technique AXE_REND = add(new Technique("axe_rend", Archetype.AXE, GestureShape.LINE_V_UP, 4, 25, "axe_t2", 12, new BleedEffect(2.7, 105, 1.0, 2, 60)));
    public static final Technique AXE_CYCLONE = add(new Technique("axe_cyclone", Archetype.AXE, GestureShape.CIRCLE, 7, 35, "axe_t3", 15, new SpinEffect(3.5, 1.0, 2, 10)));
    public static final Technique AXE_QUAKE = add(new Technique("axe_quake", Archetype.AXE, GestureShape.ZIGZAG, 10, 55, "axe_t4", 20, new GroundSlamEffect(4.2, 1.3, 0.5)));
    public static final Technique AXE_STEP = add(new Technique("axe_step", Archetype.AXE, GestureShape.CARET, 1, 28, null, 9,
            new DashStrikeEffect(3.2, 150, 1.05, 3.6, 0.0, 3, (player, target, damage, index) -> damage)));
    public static final Technique AXE_BRACE = add(new Technique("axe_brace", Archetype.AXE, GestureShape.V, 1, 160, null, 10,
            new BraceEffect(10)));

    public static final Technique SPEAR_THRUST = add(new Technique("spear_thrust", Archetype.SPEAR, GestureShape.LINE_V_UP, 1, 13, null, 6, new LinePierceEffect(4.4, 0.7, 1.0, 0.3)));
    public static final Technique SPEAR_LUNGE = add(new Technique("spear_lunge", Archetype.SPEAR, GestureShape.LINE_H, 1, 14, "spear_t1", 8, new LinePierceEffect(5.2, 0.85, 1.0, 0.55)));
    public static final Technique SPEAR_IMPALE = add(new Technique("spear_impale", Archetype.SPEAR, GestureShape.ZIGZAG, 4, 30, "spear_t2", 13,
            new FamilyFlurryEffect(4.2, 30, 0.75, 3, 5, 0.1, 0.0, (player, target, damage, index) -> damage)));
    public static final Technique SPEAR_TEMPEST = add(new Technique("spear_tempest", Archetype.SPEAR, GestureShape.CIRCLE, 7, 44, "spear_t3", 16, new SpinEffect(3.2, 0.95, 3, 5)));
    public static final Technique SPEAR_SKYFALL = add(new Technique("spear_skyfall", Archetype.SPEAR, GestureShape.LINE_V_DOWN, 10, 50, "spear_t4", 20,
            new FamilyArcEffect(3.4, 25, 1.5, 0.0, 0.0, (player, target, damage, index) -> damage)));
    public static final Technique SPEAR_STEP = add(new Technique("spear_step", Archetype.SPEAR, GestureShape.CARET, 1, 26, null, 8,
            new DashStrikeEffect(4.2, 50, 1.0, 5.4, 0.0, 2, (player, target, damage, index) -> damage)));


    public static final Technique SHIELD_BASH = add(new Technique("shield_bash", Archetype.SHIELD, GestureShape.LINE_H, 1, 20, null, 8, new ShieldBashEffect()));
    public static final Technique SHIELD_GUARD = add(new Technique("shield_guard", Archetype.SHIELD, GestureShape.LINE_V_DOWN, 1, 200, null, 10, new ShieldGuardEffect(40)));
    public static final Technique SHIELD_CHARGE = add(new Technique("shield_charge", Archetype.SHIELD, GestureShape.LINE_V_UP, 1, 60, null, 12, new ShieldChargeEffect(60)));
    public static final Technique SHIELD_THROW = add(new Technique("shield_throw", Archetype.SHIELD, GestureShape.CIRCLE, 1, 120, null, 14, new ShieldThrowEffect(14.0, 3, 100)));

    public static final Technique BOW_FIRE = add(new Technique("bow_fire", Archetype.BOW, GestureShape.CIRCLE, 1, 80, null, 12, new BowFireEffect()));

    private static final Set<String> OVERLAY_FAMILIES = Set.of("sword");

    private Techniques() {
    }

    static {
        applyConfig();
    }

    private static Technique add(Technique technique) {
        BY_ID.put(technique.id(), technique);
        BY_ARCHETYPE.computeIfAbsent(technique.archetype(), k -> new EnumMap<>(GestureShape.class)).put(technique.shape(), technique);
        return technique;
    }

    @Nullable
    public static Technique get(String id) {
        return BY_ID.get(id);
    }

    public static Collection<Technique> all() {
        return BY_ID.values();
    }

    public static List<Technique> of(Archetype archetype) {
        List<Technique> list = new ArrayList<>();
        for (Technique technique : BY_ID.values()) {
            if (technique.archetype() == archetype) {
                list.add(technique);
            }
        }
        return list;
    }

    public static void applyConfig() {
        for (Technique technique : BY_ID.values()) {
            AbilityConfig config = AbilityConfigs.of(technique);
            technique.setArchetype(config == null ? null : config.archetype());
            technique.setShape(config == null || config.gestures().isEmpty() ? null : config.gestures().get(0));
        }
        BY_ARCHETYPE.clear();
        for (Technique technique : BY_ID.values()) {
            AbilityConfig config = AbilityConfigs.of(technique);
            if (config != null && !config.gestures().isEmpty()) {
                continue;
            }
            if (technique.family() == null && technique.archetype() != null && technique.shape() != null) {
                BY_ARCHETYPE.computeIfAbsent(technique.archetype(), k -> new EnumMap<>(GestureShape.class))
                        .putIfAbsent(technique.shape(), technique);
            }
        }
        for (Technique technique : BY_ID.values()) {
            AbilityConfig config = AbilityConfigs.of(technique);
            if (config == null || config.gestures().isEmpty() || technique.archetype() == null
                    || technique.family() != null) {
                continue;
            }
            Map<GestureShape, Technique> map = BY_ARCHETYPE.computeIfAbsent(technique.archetype(), k -> new EnumMap<>(GestureShape.class));
            for (GestureShape shape : config.gestures()) {
                map.put(shape, technique);
            }
        }
        BY_FAMILY.clear();
        for (Technique technique : BY_ID.values()) {
            if (technique.family() == null || technique.shape() == null) {
                continue;
            }
            BY_FAMILY.computeIfAbsent(technique.family(), k -> new EnumMap<>(GestureShape.class))
                    .put(technique.shape(), technique);
        }
        audit();
    }

    private static void audit() {
        Map<String, Archetype> familyArchetype = new java.util.LinkedHashMap<>();
        java.util.List<String> problems = new java.util.ArrayList<>();
        for (Technique technique : BY_ID.values()) {
            String family = technique.family();
            Archetype archetype = technique.archetype();
            if (family == null || archetype == null || technique.shape() == null) {
                continue;
            }
            Archetype known = familyArchetype.putIfAbsent(family, archetype);
            if (known != null && known != archetype) {
                problems.add(family + " mixes archetypes " + known + "/" + archetype);
            }
            if (com.putzwirk.gesturalcombat.weapon.WeaponRules.heavy(family)
                    && com.putzwirk.gesturalcombat.weapon.WeaponRules.heavyLocked().contains(technique.shape())) {
                problems.add(technique.id() + " can never fire: " + family + " is heavy and locks " + technique.shape());
            }
        }
        for (String problem : problems) {
            com.putzwirk.gesturalcombat.Gesturalcombat.LOGGER.warn("ability audit: {}", problem);
        }
    }

    @Nullable
    public static String family(ItemStack stack) {
        return com.putzwirk.gesturalcombat.weapon.WeaponProfiles.family(stack);
    }

    @Nullable
    public static Technique familyTechnique(String family, GestureShape shape) {
        if (family == null || shape == null) {
            return null;
        }
        Map<GestureShape, Technique> map = BY_FAMILY.get(family.toLowerCase(java.util.Locale.ROOT));
        return map == null ? null : map.get(shape);
    }

    @Nullable
    public static Technique forGesture(Archetype archetype, GestureShape shape) {
        return forGesture(archetype, shape, null);
    }

    @Nullable
    public static Technique available(Archetype archetype, GestureShape shape, @Nullable ItemStack stack) {
        if (archetype == null || shape == null || stack == null || stack.isEmpty()) {
            return null;
        }
        if (com.putzwirk.gesturalcombat.weapon.WeaponRules.blocked(stack, shape)) {
            return null;
        }
        if (!com.putzwirk.gesturalcombat.weapon.WeaponProfiles.allows(stack, shape)) {
            return null;
        }
        Technique technique = forGesture(archetype, shape, stack);
        if (technique == null || technique.archetype() != archetype || technique.shape() != shape) {
            return null;
        }
        return technique;
    }

    @Nullable
    public static Technique forGesture(Archetype archetype, GestureShape shape, @Nullable ItemStack stack) {
        if (archetype == null || shape == null) {
            return null;
        }
        if (stack != null && !stack.isEmpty()) {
            String family = family(stack);
            if (family != null) {
                Map<GestureShape, Technique> familyMap = BY_FAMILY.get(family);
                if (familyMap != null && !familyMap.isEmpty()) {
                    Technique technique = familyMap.get(shape);
                    if (technique != null) {
                        return technique;
                    }
                    if (!OVERLAY_FAMILIES.contains(family)) {
                        return null;
                    }
                }
            }
        }
        Map<GestureShape, Technique> map = BY_ARCHETYPE.get(archetype);
        Technique fallback = map == null ? null : map.get(shape);
        return fallback != null && fallback.archetype() == archetype ? fallback : null;
    }

    @Nullable
    public static Technique basic(Archetype archetype) {
        for (Technique technique : BY_ID.values()) {
            if (technique.archetype() == archetype && technique.nodeId() == null) {
                return technique;
            }
        }
        return null;
    }
}
