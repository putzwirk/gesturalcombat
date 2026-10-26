package com.putzwirk.gesturalcombat.weapon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.putzwirk.gesturalcombat.core.GestureShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponDataTest {
    private static final String[] FILES = {
            "00-categories.json", "10-vanilla.json", "20-simplyswords.json", "30-born-in-chaos.json",
            "40-misc.json"
    };

    private static final Set<String> GESTURES = new HashSet<>();

    static {
        for (GestureShape shape : GestureShape.STRIP) {
            GESTURES.add(shape.id());
        }
    }

    @Test
    void everyProfileOnlyListsKnownGestures() throws IOException {
        List<JsonObject> profiles = new ArrayList<>();
        for (String file : FILES) {
            JsonObject doc = read("/data/gesturalcombat/weapons/" + file);
            assertNotNull(doc, "missing " + file);
            profiles.addAll(profilesOf(doc));
        }
        assertFalse(profiles.isEmpty(), "no weapon profiles loaded");
        for (JsonObject profile : profiles) {
            if (!profile.has("gestures") || !profile.get("gestures").isJsonArray()) {
                continue;
            }
            JsonArray gestures = profile.getAsJsonArray("gestures");
            for (JsonElement element : gestures) {
                assertTrue(GESTURES.contains(element.getAsString()),
                        "unknown gesture " + element.getAsString() + " in " + profile);
            }
        }
    }

    @Test
    void everyProfilePinsTwoHandedness() throws IOException {
        List<JsonObject> profiles = new ArrayList<>();
        for (String file : FILES) {
            JsonObject doc = read("/data/gesturalcombat/weapons/" + file);
            assertNotNull(doc, "missing " + file);
            profiles.addAll(profilesOf(doc));
        }
        assertFalse(profiles.isEmpty(), "no weapon profiles loaded");
        for (JsonObject profile : profiles) {
            if (profilesOf(profile).isEmpty() && profile.has("items") && !profile.getAsJsonArray("items").isEmpty()) {
                assertTrue(profile.has("two_handed"),
                        "profile without pinned two_handed: " + profile.getAsJsonArray("items"));
            }
        }
    }

    @Test
    void bornInChaosDropsCaretEverywhere() throws IOException {
        JsonObject doc = read("/data/gesturalcombat/weapons/30-born-in-chaos.json");
        assertNotNull(doc);
        List<JsonObject> profiles = profilesOf(doc);
        assertFalse(profiles.isEmpty());
        for (JsonObject profile : profiles) {
            assertTrue(profile.has("gestures"), "profile without allow-list: " + profile);
            for (JsonElement element : profile.getAsJsonArray("gestures")) {
                assertFalse("caret".equals(element.getAsString()), "caret still allowed in " + profile);
            }
        }
    }

    private static List<JsonObject> profilesOf(JsonObject doc) {
        List<JsonObject> result = new ArrayList<>();
        if (doc.has("profiles") && doc.get("profiles").isJsonArray()) {
            for (JsonElement element : doc.getAsJsonArray("profiles")) {
                if (element.isJsonObject()) {
                    result.add(element.getAsJsonObject());
                }
            }
        }
        if (doc.has("weapons") && doc.get("weapons").isJsonArray()) {
            for (JsonElement element : doc.getAsJsonArray("weapons")) {
                if (element.isJsonObject()) {
                    result.add(element.getAsJsonObject());
                }
            }
        }
        return result;
    }

    private static JsonObject read(String path) throws IOException {
        try (InputStream stream = WeaponDataTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
