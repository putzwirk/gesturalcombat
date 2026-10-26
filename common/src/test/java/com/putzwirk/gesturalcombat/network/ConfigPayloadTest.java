package com.putzwirk.gesturalcombat.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPayloadTest {
    @Test
    void roundTripsJson() {
        String json = "{\"abilities\":[1,2,3],\"name\":\"gesturalcombat\"}";
        byte[] packed = ConfigPayload.pack(json);
        assertTrue(packed.length > 0);
        assertEquals(json, ConfigPayload.unpack(packed));
    }

    @Test
    void emptyInputStaysEmpty() {
        assertEquals(0, ConfigPayload.pack(null).length);
        assertEquals(0, ConfigPayload.pack("   ").length);
        assertEquals("", ConfigPayload.unpack(new byte[0]));
        assertEquals("", ConfigPayload.unpack(null));
    }

    @Test
    void corruptInputFallsBackToEmpty() {
        assertEquals("", ConfigPayload.unpack(new byte[]{1, 2, 3, 4}));
    }
}
