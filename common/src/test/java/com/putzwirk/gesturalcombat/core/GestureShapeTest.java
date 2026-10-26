package com.putzwirk.gesturalcombat.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestureShapeTest {
    @Test
    void stripHasNoDuplicatesAndExcludesUnknown() {
        Set<GestureShape> seen = new HashSet<>();
        for (GestureShape shape : GestureShape.STRIP) {
            assertTrue(seen.add(shape), "duplicate slot " + shape);
            assertTrue(shape.isKnown());
        }
        assertFalse(seen.contains(GestureShape.UNKNOWN));
        assertEquals(7, GestureShape.STRIP.length);
    }

    @Test
    void idsAreStable() {
        assertEquals("line_h", GestureShape.LINE_H.id());
        assertEquals("line_v_up", GestureShape.LINE_V_UP.id());
        assertEquals("line_v_down", GestureShape.LINE_V_DOWN.id());
        assertEquals("circle", GestureShape.CIRCLE.id());
        assertEquals("zigzag", GestureShape.ZIGZAG.id());
        assertEquals("v", GestureShape.V.id());
        assertEquals("caret", GestureShape.CARET.id());
        assertEquals("gesture.gesturalcombat.circle", GestureShape.CIRCLE.nameKey());
    }

    @Test
    void colorsAreOpaque() {
        for (GestureShape shape : GestureShape.values()) {
            assertEquals(0xFF000000, shape.color() & 0xFF000000);
        }
    }
}
