package com.putzwirk.gesturalcombat.core;

public enum GestureShape {
    LINE_H("line_h", 0xFFFFFF),
    LINE_V_UP("line_v_up", 0xFFFFFF),
    LINE_V_DOWN("line_v_down", 0xFFFFFF),
    CIRCLE("circle", 0xFFFFFF),
    ZIGZAG("zigzag", 0xFFFFFF),
    V("v", 0xFFFFFF),
    CARET("caret", 0xFFFFFF),
    UNKNOWN("unknown", 0x808080);

    public static final GestureShape[] STRIP = {
            LINE_H, LINE_V_UP, LINE_V_DOWN, CIRCLE, ZIGZAG, V, CARET
    };

    private final String id;
    private final int color;

    GestureShape(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return 0xFF000000 | color;
    }

    public String nameKey() {
        return "gesture.gesturalcombat." + id;
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }
}
