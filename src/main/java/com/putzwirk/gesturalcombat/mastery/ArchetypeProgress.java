package com.putzwirk.gesturalcombat.mastery;

import net.minecraft.nbt.CompoundTag;

public class ArchetypeProgress {
    private int level = 1;
    private int xp = 0;
    private int points = 1;

    public int level() {
        return level;
    }

    public int xp() {
        return xp;
    }

    public int points() {
        return points;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void spendPoint() {
        if (points > 0) {
            points--;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("points", points);
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Math.max(1, tag.getInt("level"));
        xp = Math.max(0, tag.getInt("xp"));
        points = Math.max(0, tag.getInt("points"));
    }

    public void copyFrom(ArchetypeProgress other) {
        level = other.level;
        xp = other.xp;
        points = other.points;
    }
}
