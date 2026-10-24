package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.effect.ArcSweepEffect;
import com.putzwirk.gesturalcombat.technique.effect.BleedEffect;
import com.putzwirk.gesturalcombat.technique.effect.BowFireEffect;
import com.putzwirk.gesturalcombat.technique.effect.FlurryEffect;
import com.putzwirk.gesturalcombat.technique.effect.GlaiveSweepEffect;
import com.putzwirk.gesturalcombat.technique.effect.GroundSlamEffect;
import com.putzwirk.gesturalcombat.technique.effect.DeathblowEffect;
import com.putzwirk.gesturalcombat.technique.effect.GuardEffect;
import com.putzwirk.gesturalcombat.technique.effect.LinePierceEffect;
import com.putzwirk.gesturalcombat.technique.effect.MomentumDumpEffect;
import com.putzwirk.gesturalcombat.technique.effect.MomentumSweepEffect;
import com.putzwirk.gesturalcombat.technique.effect.PostureEffect;
import com.putzwirk.gesturalcombat.technique.effect.PostureFlurryEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldBashEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldChargeEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldGuardEffect;
import com.putzwirk.gesturalcombat.technique.effect.ShieldThrowEffect;
import com.putzwirk.gesturalcombat.technique.effect.SkirmishEffect;
import com.putzwirk.gesturalcombat.technique.effect.SpinEffect;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Techniques {
    private static final Map<String, Technique> BY_ID = new LinkedHashMap<>();
    private static final Map<Archetype, Map<GestureShape, Technique>> BY_ARCHETYPE = new EnumMap<>(Archetype.class);
    private static final Map<String, Map<GestureShape, Technique>> BY_FAMILY = new LinkedHashMap<>();

    public static final Technique SWORD_SLASH = add(new Technique("sword_slash", Archetype.SWORD, GestureShape.LINE_H, 1, 10, null, 6, new ArcSweepEffect(2.8, 110, 1.0, 0.0, 0.25)));
    public static final Technique SWORD_RISING = add(new Technique("sword_rising", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 16, "sword_t1", 8, new ArcSweepEffect(2.8, 120, 1.25, 0.55, 0.0)));
    public static final Technique SWORD_WHIRL = add(new Technique("sword_whirl", Archetype.SWORD, GestureShape.CIRCLE, 4, 30, "sword_t2", 12, new SpinEffect(3.1, 0.85, 2, 8)));
    public static final Technique SWORD_FLURRY = add(new Technique("sword_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 7, 40, "sword_t3", 14, new FlurryEffect(2.9, 90, 1.4, 4, 4)));
    public static final Technique SWORD_EXECUTE = add(new Technique("sword_execute", Archetype.SWORD, GestureShape.LINE_V_DOWN, 10, 50, "sword_t4", 20, new ArcSweepEffect(2.6, 70, 2.1, 0.0, 0.5)));
    public static final Technique SWORD_GUARD = add(new Technique("sword_guard", Archetype.SWORD, GestureShape.V, 1, 200, null, 10, new GuardEffect(20, 2)));
    public static final Technique SWORD_SKIRMISH = add(new Technique("sword_skirmish", Archetype.SWORD, GestureShape.CARET, 1, 60, null, 10, new SkirmishEffect(3.9, 120, 1.05, 2.6, 12)));

    public static final Technique KATANA_SLASH = add(new Technique("katana_slash", Archetype.SWORD, GestureShape.LINE_H, 1, 12, null, 7, new PostureEffect(3.1, 100, 0.9, 18), "katana"));
    public static final Technique KATANA_RISING = add(new Technique("katana_rising", Archetype.SWORD, GestureShape.LINE_V_UP, 1, 16, null, 8, new PostureEffect(2.6, 120, 1.15, 15, 0.4), "katana"));
    public static final Technique KATANA_WHIRL = add(new Technique("katana_whirl", Archetype.SWORD, GestureShape.CIRCLE, 4, 30, null, 12, new PostureFlurryEffect(3.1, 360, 0.8, 2, 8, 12), "katana"));
    public static final Technique KATANA_FLURRY = add(new Technique("katana_flurry", Archetype.SWORD, GestureShape.ZIGZAG, 7, 40, null, 14, new PostureFlurryEffect(3.1, 90, 1.8, 4, 4, 9), "katana"));
    public static final Technique KATANA_EXECUTE = add(new Technique("katana_execute", Archetype.SWORD, GestureShape.LINE_V_DOWN, 10, 50, null, 20, new DeathblowEffect(2.6, 110, 2.4), "katana"));

    public static final Technique GLAIVE_SWEEP = add(new Technique("glaive_sweep", Archetype.SPEAR, GestureShape.LINE_H, 1, 14, "spear_t1", 8, new GlaiveSweepEffect(3.75, 150, 0.75), "glaive"));

    public static final Technique GREATAXE_SWEEP = add(new Technique("greataxe_sweep", Archetype.AXE, GestureShape.LINE_H, 1, 16, null, 8, new MomentumSweepEffect(4.0, 200, 0.9), "greataxe"));
    public static final Technique GREATAXE_DUMP = add(new Technique("greataxe_dump", Archetype.AXE, GestureShape.CARET, 4, 30, null, 12, new MomentumDumpEffect(4.0), "greataxe"));

    public static final Technique AXE_CLEAVE = add(new Technique("axe_cleave", Archetype.AXE, GestureShape.LINE_V_DOWN, 1, 12, null, 6, new ArcSweepEffect(2.9, 95, 1.15, 0.0, 0.35)));
    public static final Technique AXE_WIDE = add(new Technique("axe_wide", Archetype.AXE, GestureShape.LINE_H, 1, 20, "axe_t1", 9, new ArcSweepEffect(3.5, 175, 0.9, 0.0, 0.6)));
    public static final Technique AXE_REND = add(new Technique("axe_rend", Archetype.AXE, GestureShape.LINE_V_UP, 4, 25, "axe_t2", 12, new BleedEffect(2.7, 105, 1.0, 2, 60)));
    public static final Technique AXE_CYCLONE = add(new Technique("axe_cyclone", Archetype.AXE, GestureShape.CIRCLE, 7, 35, "axe_t3", 15, new SpinEffect(3.5, 1.0, 2, 10)));
    public static final Technique AXE_QUAKE = add(new Technique("axe_quake", Archetype.AXE, GestureShape.ZIGZAG, 10, 55, "axe_t4", 20, new GroundSlamEffect(4.2, 1.3, 0.5)));

    public static final Technique SPEAR_THRUST = add(new Technique("spear_thrust", Archetype.SPEAR, GestureShape.LINE_V_UP, 1, 9, null, 6, new LinePierceEffect(4.4, 1.1, 1.0, 0.3)));
    public static final Technique SPEAR_SWEEP = add(new Technique("spear_sweep", Archetype.SPEAR, GestureShape.LINE_H, 1, 15, "spear_t1", 8, new ArcSweepEffect(3.7, 130, 0.85, 0.0, 0.3)));
    public static final Technique SPEAR_IMPALE = add(new Technique("spear_impale", Archetype.SPEAR, GestureShape.ZIGZAG, 4, 28, "spear_t2", 13, new FlurryEffect(3.8, 45, 0.65, 3, 5)));
    public static final Technique SPEAR_TEMPEST = add(new Technique("spear_tempest", Archetype.SPEAR, GestureShape.CIRCLE, 7, 42, "spear_t3", 16, new SpinEffect(3.9, 0.9, 3, 7)));
    public static final Technique SPEAR_SKYFALL = add(new Technique("spear_skyfall", Archetype.SPEAR, GestureShape.LINE_V_DOWN, 10, 50, "spear_t4", 20, new ArcSweepEffect(3.1, 90, 1.7, 0.8, 0.0)));


    public static final Technique SHIELD_BASH = add(new Technique("shield_bash", Archetype.SHIELD, GestureShape.LINE_H, 1, 20, null, 8, new ShieldBashEffect()));
    public static final Technique SHIELD_GUARD = add(new Technique("shield_guard", Archetype.SHIELD, GestureShape.LINE_V_DOWN, 1, 200, null, 10, new ShieldGuardEffect(40)));
    public static final Technique SHIELD_CHARGE = add(new Technique("shield_charge", Archetype.SHIELD, GestureShape.LINE_V_UP, 1, 60, null, 12, new ShieldChargeEffect(60)));
    public static final Technique SHIELD_THROW = add(new Technique("shield_throw", Archetype.SHIELD, GestureShape.CIRCLE, 1, 120, null, 14, new ShieldThrowEffect(14.0, 3, 100)));

    public static final Technique BOW_FIRE = add(new Technique("bow_fire", Archetype.BOW, GestureShape.CIRCLE, 1, 80, null, 12, new BowFireEffect()));

    private Techniques() {
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
    }

    @Nullable
    public static String family(ItemStack stack) {
        return com.putzwirk.gesturalcombat.weapon.WeaponProfiles.family(stack);
    }

    @Nullable
    public static Technique forGesture(Archetype archetype, GestureShape shape) {
        return forGesture(archetype, shape, null);
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
                if (familyMap != null) {
                    Technique technique = familyMap.get(shape);
                    if (technique != null && technique.archetype() == archetype) {
                        return technique;
                    }
                }
            }
        }
        Map<GestureShape, Technique> map = BY_ARCHETYPE.get(archetype);
        return map == null ? null : map.get(shape);
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
