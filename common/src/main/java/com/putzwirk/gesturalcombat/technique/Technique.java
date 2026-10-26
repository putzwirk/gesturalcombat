package com.putzwirk.gesturalcombat.technique;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;

public final class Technique {
    private final String id;
    private final Archetype baseArchetype;
    private final GestureShape baseShape;
    private Archetype archetype;
    private GestureShape shape;
    private final int requiredLevel;
    private final int cooldownTicks;
    private final String nodeId;
    private final int xp;
    private final TechniqueEffect effect;
    @javax.annotation.Nullable
    private final String family;

    public Technique(String id, Archetype archetype, GestureShape shape, int requiredLevel, int cooldownTicks, String nodeId, int xp, TechniqueEffect effect) {
        this(id, archetype, shape, requiredLevel, cooldownTicks, nodeId, xp, effect, null);
    }

    public Technique(String id, Archetype archetype, GestureShape shape, int requiredLevel, int cooldownTicks, String nodeId, int xp, TechniqueEffect effect, @javax.annotation.Nullable String family) {
        this.id = id;
        this.baseArchetype = archetype;
        this.baseShape = shape;
        this.archetype = archetype;
        this.shape = shape;
        this.requiredLevel = requiredLevel;
        this.cooldownTicks = cooldownTicks;
        this.nodeId = nodeId;
        this.xp = xp;
        this.effect = effect;
        this.family = family == null ? null : family.toLowerCase(java.util.Locale.ROOT);
    }

    public String id() {
        return id;
    }

    public Archetype baseArchetype() {
        return baseArchetype;
    }

    public GestureShape baseShape() {
        return baseShape;
    }

    public void setArchetype(Archetype value) {
        this.archetype = value == null ? baseArchetype : value;
    }

    public void setShape(GestureShape value) {
        this.shape = value == null ? baseShape : value;
    }

    public Archetype archetype() {
        return archetype;
    }

    public GestureShape shape() {
        return shape;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public String nodeId() {
        return nodeId;
    }

    public int xp() {
        return xp;
    }

    public TechniqueEffect effect() {
        return effect;
    }

    @javax.annotation.Nullable
    public String family() {
        return family;
    }

    public boolean gatedByNode() {
        return nodeId != null;
    }
}
