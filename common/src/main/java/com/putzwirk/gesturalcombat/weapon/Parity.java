package com.putzwirk.gesturalcombat.weapon;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.client.anim.AnimationCatalog;
import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;

import java.util.ArrayList;
import java.util.List;

public final class Parity {
    public static final String TAG = "parity:";

    private Parity() {
    }

    public static List<String> lines() {
        AbilityConfigs.ensureLoaded();
        List<String> lines = new ArrayList<>();
        for (WeaponAudit.Report report : WeaponAudit.scan()) {
            lines.add(line(report));
        }
        return lines;
    }

    public static String line(WeaponAudit.Report report) {
        StringBuilder text = new StringBuilder();
        text.append(report.item())
                .append(" family=").append(report.family() == null ? "-" : report.family())
                .append(" archetype=").append(report.archetype())
                .append(" profile=").append(report.profile() ? 1 : 0)
                .append(" two_handed=").append(AnimationCatalog.twoHanded(report.stack()) ? 1 : 0)
                .append(" reach=").append(number(report.reach()))
                .append(" pool=").append(String.join(",", AnimationCatalog.poolFor(report.stack())));
        for (WeaponAudit.Slot slot : report.slots()) {
            text.append(' ').append(slot.shape().id()).append('=');
            Technique technique = Techniques.get(slot.technique());
            if (technique == null) {
                text.append("unresolved");
                continue;
            }
            int swings = AnimationCatalog.swingCount(technique, report.family());
            String[] sequence = AnimationCatalog.sequenceFor(report.archetype(), slot.shape(),
                    report.stack(), 1, swings);
            text.append(technique.id()).append('/').append(swings).append(':');
            if (sequence == null || sequence.length == 0) {
                text.append('-');
            } else {
                for (int i = 0; i < sequence.length; i++) {
                    if (i > 0) {
                        text.append('+');
                    }
                    String id = sequence[i];
                    text.append(BetterCombatBridge.animation(id) == null ? id + "!" : id);
                }
            }
        }
        return text.toString();
    }

    public static List<String> missingAnimations() {
        List<String> missing = new ArrayList<>();
        for (WeaponAudit.Report report : WeaponAudit.scan()) {
            for (String id : referenced(report)) {
                if (BetterCombatBridge.animation(id) == null && !missing.contains(id)) {
                    missing.add(id);
                }
            }
        }
        return missing;
    }

    private static List<String> referenced(WeaponAudit.Report report) {
        List<String> ids = new ArrayList<>();
        for (String id : WeaponProfiles.animations(report.stack())) {
            if (id != null && !id.isBlank() && !ids.contains(id)) {
                ids.add(id);
            }
        }
        String family = report.family();
        String key = family == null || family.isBlank() ? BetterCombatBridge.category(report.stack()) : family;
        for (String id : com.putzwirk.gesturalcombat.technique.AbilityConfigs.categoryAnimations(key)) {
            if (id != null && !id.isBlank() && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static boolean driftReported;

    public static void reportDrift() {
        if (driftReported) {
            return;
        }
        driftReported = true;
        List<String> missing = missingAnimations();
        if (missing.isEmpty()) {
            Gesturalcombat.LOGGER.info("gesturalcombat {} every pinned animation is registered", TAG);
            return;
        }
        Gesturalcombat.LOGGER.warn("gesturalcombat {} {} pinned animation(s) are not registered, those casts will play no animation: {}",
                TAG, missing.size(), String.join(" ", missing));
    }

    public static void log() {
        for (String line : lines()) {
            Gesturalcombat.LOGGER.info("gesturalcombat {} {}", TAG, line);
        }
        List<String> missing = missingAnimations();
        if (missing.isEmpty()) {
            Gesturalcombat.LOGGER.info("gesturalcombat {} every profile animation is registered", TAG);
            return;
        }
        Gesturalcombat.LOGGER.warn("gesturalcombat {} {} profile animation(s) are not registered: {}",
                TAG, missing.size(), String.join(" ", missing));
    }

    private static String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
