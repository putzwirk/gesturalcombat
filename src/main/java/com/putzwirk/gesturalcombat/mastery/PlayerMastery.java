package com.putzwirk.gesturalcombat.mastery;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.skill.Passive;
import com.putzwirk.gesturalcombat.skill.SkillNode;
import com.putzwirk.gesturalcombat.technique.Technique;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerMastery {
    public static final int MAX_LEVEL = 20;

    private final Map<Archetype, ArchetypeProgress> progress = new EnumMap<>(Archetype.class);
    private final Set<String> unlocked = new HashSet<>();

    public PlayerMastery() {
        for (Archetype archetype : Archetype.values()) {
            progress.put(archetype, new ArchetypeProgress());
        }
    }

    public static int xpForNext(int level) {
        return 60 + 40 * Math.max(1, level);
    }

    public ArchetypeProgress progress(Archetype archetype) {
        return progress.computeIfAbsent(archetype, k -> new ArchetypeProgress());
    }

    public int level(Archetype archetype) {
        return progress(archetype).level();
    }

    public int xp(Archetype archetype) {
        return progress(archetype).xp();
    }

    public int points(Archetype archetype) {
        return progress(archetype).points();
    }

    public int xpToNext(Archetype archetype) {
        ArchetypeProgress p = progress(archetype);
        return p.level() >= MAX_LEVEL ? -1 : xpForNext(p.level());
    }

    public boolean addXp(Archetype archetype, int amount) {
        if (amount <= 0) {
            return false;
        }
        ArchetypeProgress p = progress(archetype);
        if (p.level() >= MAX_LEVEL) {
            return false;
        }
        p.setXp(p.xp() + amount);
        boolean leveled = false;
        while (p.level() < MAX_LEVEL && p.xp() >= xpForNext(p.level())) {
            p.setXp(p.xp() - xpForNext(p.level()));
            p.setLevel(p.level() + 1);
            p.setPoints(p.points() + 1);
            leveled = true;
        }
        if (p.level() >= MAX_LEVEL) {
            p.setXp(0);
        }
        return leveled;
    }

    public boolean isUnlocked(String nodeId) {
        return nodeId != null && unlocked.contains(nodeId);
    }

    public boolean canUnlock(SkillNode node) {
        if (node == null || unlocked.contains(node.id())) {
            return false;
        }
        ArchetypeProgress p = progress(node.archetype());
        if (p.level() < node.requiredLevel() || p.points() <= 0) {
            return false;
        }
        return node.prerequisiteId() == null || unlocked.contains(node.prerequisiteId());
    }

    public boolean unlock(SkillNode node) {
        if (!canUnlock(node)) {
            return false;
        }
        progress(node.archetype()).spendPoint();
        unlocked.add(node.id());
        return true;
    }

    public boolean isTechniqueAvailable(Technique technique) {
        return technique != null;
    }

    public float passive(Archetype archetype, Passive passive) {
        float total = 0.0f;
        for (String id : unlocked) {
            SkillNode node = com.putzwirk.gesturalcombat.skill.SkillTree.get(id);
            if (node != null && node.archetype() == archetype && node.passive() == passive) {
                total += node.passiveValue();
            }
        }
        return total;
    }

    public void copyFrom(PlayerMastery other) {
        for (Archetype archetype : Archetype.values()) {
            progress(archetype).copyFrom(other.progress(archetype));
        }
        unlocked.clear();
        unlocked.addAll(other.unlocked);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag progressTag = new CompoundTag();
        for (Archetype archetype : Archetype.values()) {
            progressTag.put(archetype.id(), progress(archetype).save());
        }
        tag.put("progress", progressTag);
        ListTag list = new ListTag();
        for (String id : unlocked) {
            list.add(StringTag.valueOf(id));
        }
        tag.put("unlocked", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("progress", Tag.TAG_COMPOUND)) {
            CompoundTag progressTag = tag.getCompound("progress");
            for (Archetype archetype : Archetype.values()) {
                if (progressTag.contains(archetype.id(), Tag.TAG_COMPOUND)) {
                    progress(archetype).load(progressTag.getCompound(archetype.id()));
                }
            }
        }
        unlocked.clear();
        ListTag list = tag.getList("unlocked", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            unlocked.add(list.getString(i));
        }
    }
}
