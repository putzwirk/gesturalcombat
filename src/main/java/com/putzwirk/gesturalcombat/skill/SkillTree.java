package com.putzwirk.gesturalcombat.skill;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillTree {
    private static final Map<String, SkillNode> BY_ID = new LinkedHashMap<>();
    private static final Map<Archetype, List<SkillNode>> BY_ARCHETYPE = new EnumMap<>(Archetype.class);

    static {
        tree(Archetype.SWORD, "sword", Techniques.SWORD_RISING, Techniques.SWORD_WHIRL, Techniques.SWORD_FLURRY, Techniques.SWORD_EXECUTE);
        tree(Archetype.AXE, "axe", Techniques.AXE_WIDE, Techniques.AXE_REND, Techniques.AXE_CYCLONE, Techniques.AXE_QUAKE);
        tree(Archetype.SPEAR, "spear", Techniques.SPEAR_LUNGE, Techniques.SPEAR_IMPALE, Techniques.SPEAR_TEMPEST, Techniques.SPEAR_SKYFALL);
    }

    private SkillTree() {
    }

    private static void tree(Archetype archetype, String prefix, Technique t1, Technique t2, Technique t3, Technique t4) {
        add(new SkillNode(prefix + "_t1", archetype, 1, 0, 0, null, t1, null, 0));
        add(new SkillNode(prefix + "_p1", archetype, 3, 1, 0, prefix + "_t1", null, Passive.DAMAGE, 0.10f));
        add(new SkillNode(prefix + "_t2", archetype, 4, 1, 1, prefix + "_t1", t2, null, 0));
        add(new SkillNode(prefix + "_t3", archetype, 7, 2, 0, prefix + "_t2", t3, null, 0));
        add(new SkillNode(prefix + "_p2", archetype, 8, 2, 1, prefix + "_t3", null, Passive.COOLDOWN, 0.15f));
        add(new SkillNode(prefix + "_t4", archetype, 10, 3, 0, prefix + "_t3", t4, null, 0));
        add(new SkillNode(prefix + "_p3", archetype, 12, 3, 1, prefix + "_t4", null, Passive.RANGE, 0.15f));
    }

    private static void add(SkillNode node) {
        BY_ID.put(node.id(), node);
        BY_ARCHETYPE.computeIfAbsent(node.archetype(), k -> new ArrayList<>()).add(node);
    }

    @Nullable
    public static SkillNode get(String id) {
        return BY_ID.get(id);
    }

    public static Collection<SkillNode> all() {
        return BY_ID.values();
    }

    public static List<SkillNode> of(Archetype archetype) {
        return BY_ARCHETYPE.getOrDefault(archetype, List.of());
    }

    public static int maxColumn(Archetype archetype) {
        int max = 0;
        for (SkillNode node : of(archetype)) {
            max = Math.max(max, node.column());
        }
        return max;
    }
}
