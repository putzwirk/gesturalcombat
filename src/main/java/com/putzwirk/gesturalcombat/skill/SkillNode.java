package com.putzwirk.gesturalcombat.skill;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.technique.Technique;
import javax.annotation.Nullable;

public final class SkillNode {
    private final String id;
    private final Archetype archetype;
    private final int requiredLevel;
    private final int column;
    private final int row;
    private final String prerequisiteId;
    private final Technique technique;
    private final Passive passive;
    private final float passiveValue;

    public SkillNode(String id, Archetype archetype, int requiredLevel, int column, int row, @Nullable String prerequisiteId, @Nullable Technique technique, @Nullable Passive passive, float passiveValue) {
        this.id = id;
        this.archetype = archetype;
        this.requiredLevel = requiredLevel;
        this.column = column;
        this.row = row;
        this.prerequisiteId = prerequisiteId;
        this.technique = technique;
        this.passive = passive;
        this.passiveValue = passiveValue;
    }

    public String id() {
        return id;
    }

    public Archetype archetype() {
        return archetype;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public int column() {
        return column;
    }

    public int row() {
        return row;
    }

    @Nullable
    public String prerequisiteId() {
        return prerequisiteId;
    }

    @Nullable
    public Technique technique() {
        return technique;
    }

    @Nullable
    public Passive passive() {
        return passive;
    }

    public float passiveValue() {
        return passiveValue;
    }

    public String nameKey() {
        return "node.gesturalcombat." + id;
    }

    public String descKey() {
        return "node.gesturalcombat." + id + ".desc";
    }
}
