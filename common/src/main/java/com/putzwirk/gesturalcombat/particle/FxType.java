package com.putzwirk.gesturalcombat.particle;

public enum FxType {
    RING("ring"),
    BURST("burst"),
    WAVE("wave"),
    STARS("stars");

    private final String id;

    FxType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
