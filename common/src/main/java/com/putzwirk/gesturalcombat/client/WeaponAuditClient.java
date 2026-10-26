package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.client.hud.AbilityIcons;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import com.putzwirk.gesturalcombat.weapon.WeaponAudit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WeaponAuditClient {
    private WeaponAuditClient() {
    }

    public static void run(String serverSummary) {
        boolean remote = serverSummary != null && !serverSummary.isBlank();
        if (!remote && !Config.INSTANCE.debugWeapons.get()) {
            return;
        }
        int reported = 0;
        List<WeaponAudit.Report> reports = WeaponAudit.scan();
        for (WeaponAudit.Report report : reports) {
            Set<String> drawn = new LinkedHashSet<>();
            List<String> icons = new ArrayList<>();
            List<String> gaps = new ArrayList<>();
            for (WeaponAudit.Slot slot : report.slots()) {
                Technique technique = Techniques.get(slot.technique());
                String icon = technique == null ? null : AbilityIcons.iconFor(technique);
                if (icon == null) {
                    gaps.add(slot.shape().id() + "=" + slot.technique() + " has no icon, the HUD falls back to the gesture glyph");
                } else if (!drawn.add(icon)) {
                    gaps.add(slot.shape().id() + "=" + slot.technique() + " reuses " + icon + " which an earlier slot already draws");
                } else {
                    icons.add(slot.shape().id() + "=" + icon);
                }
            }
            reported += gaps.size();
            if (gaps.isEmpty()) {
                continue;
            }
            Gesturalcombat.LOGGER.info("{} [client] {} slots={} [{}] icons=[{}] gaps={}",
                    WeaponAudit.TAG, report.item(), report.slots().size(),
                    slotText(report), String.join(" ", icons), gaps.size());
            for (String gap : gaps) {
                Gesturalcombat.LOGGER.warn("{} [client] {} [icon] {}", WeaponAudit.GAP, report.item(), gap);
            }
        }
        Gesturalcombat.LOGGER.info("{} [client] {} weapons, {} icon gaps", WeaponAudit.TAG, reports.size(), reported);
        com.putzwirk.gesturalcombat.weapon.Parity.log();
        if (remote) {
            WeaponAudit.compare(serverSummary, reports, "client");
        }
    }

    private static String slotText(WeaponAudit.Report report) {
        StringBuilder text = new StringBuilder();
        for (WeaponAudit.Slot slot : report.slots()) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(slot.shape().id()).append('=').append(slot.technique());
        }
        return text.toString();
    }
}
