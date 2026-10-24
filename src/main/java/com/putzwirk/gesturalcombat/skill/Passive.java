package com.putzwirk.gesturalcombat.skill;

public enum Passive {
    DAMAGE("damage"),
    COOLDOWN("cooldown"),
    RANGE("range");

    private final String id;

    Passive(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String nameKey() {
        return "passive.gesturalcombat." + id;
    }

    public String describe(float value) {
        return switch (this) {
            case DAMAGE -> "+" + Math.round(value * 100) + "% technique damage";
            case COOLDOWN -> "-" + Math.round(value * 100) + "% technique cooldown";
            case RANGE -> "+" + Math.round(value * 100) + "% technique reach";
        };
    }
}
