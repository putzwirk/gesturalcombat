package com.putzwirk.gesturalcombat.particle;

public enum FxType {
    SLASH45("slash45"),
    SLASH90("slash90"),
    SLASH180("slash180"),
    SLASH270("slash270"),
    SLASH360("slash360"),
    STAB("stab"),
    RING("ring"),
    BURST("burst"),
    BOLT("bolt");

    private final String id;

    FxType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
