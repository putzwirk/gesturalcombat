package com.putzwirk.gesturalcombat.client.hud;

import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class AbilityIcons {
    private static final Map<String, ResourceLocation> LOCATIONS = new HashMap<>();
    private static final Map<String, Boolean> PRESENT = new HashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        alias(Archetype.SWORD, GestureShape.LINE_H, "sword_slash");
        alias(Archetype.SWORD, GestureShape.LINE_V_UP, "sword_rising");
        alias(Archetype.SWORD, GestureShape.LINE_V_DOWN, "sword_execute");
        alias(Archetype.SWORD, GestureShape.CIRCLE, "sword_whirl");
        alias(Archetype.SWORD, GestureShape.ZIGZAG, "sword_flurry");
        alias(Archetype.SWORD, GestureShape.V, "sword_guard");
        alias(Archetype.SWORD, GestureShape.CARET, "sword_skirmish");

        alias(Archetype.AXE, GestureShape.LINE_H, "axe_wide");
        alias(Archetype.AXE, GestureShape.LINE_V_UP, "axe_rend");
        alias(Archetype.AXE, GestureShape.LINE_V_DOWN, "axe_cleave");
        alias(Archetype.AXE, GestureShape.CIRCLE, "axe_cyclone");
        alias(Archetype.AXE, GestureShape.ZIGZAG, "axe_quake");
        alias(Archetype.AXE, GestureShape.V, "axe_brace");
        alias(Archetype.AXE, GestureShape.CARET, "axe_step");

        alias(Archetype.SPEAR, GestureShape.LINE_H, "spear_sweep");
        alias(Archetype.SPEAR, GestureShape.LINE_V_UP, "spear_thrust");
        alias(Archetype.SPEAR, GestureShape.LINE_V_DOWN, "spear_skyfall");
        alias(Archetype.SPEAR, GestureShape.CIRCLE, "spear_tempest");
        alias(Archetype.SPEAR, GestureShape.ZIGZAG, "spear_impale");
        alias(Archetype.SPEAR, GestureShape.V, "spear_ward");
    }

    private AbilityIcons() {
    }

    private static void alias(Archetype archetype, GestureShape shape, String icon) {
        ALIASES.put(key(archetype, shape), icon);
    }

    private static String key(@Nullable Archetype archetype, @Nullable GestureShape shape) {
        return archetype + "/" + shape;
    }

    public static ResourceLocation location(String iconId) {
        return LOCATIONS.computeIfAbsent(iconId,
                id -> new ResourceLocation("gesturalcombat", "textures/gui/abilities/" + id + ".png"));
    }

    private static boolean present(String iconId) {
        if (iconId == null) {
            return false;
        }
        return PRESENT.computeIfAbsent(iconId,
                id -> Minecraft.getInstance().getResourceManager().getResource(location(id)).isPresent());
    }

    @Nullable
    public static String iconFor(Technique technique) {
        if (present(technique.id())) {
            return technique.id();
        }
        String alias = ALIASES.get(key(technique.archetype(), technique.shape()));
        return alias != null && present(alias) ? alias : null;
    }

    public static boolean present(Technique technique) {
        return iconFor(technique) != null;
    }

    public static void draw(GuiGraphics graphics, Technique technique, float cx, float cy, float size, int tint) {
        String icon = iconFor(technique);
        if (icon == null) {
            return;
        }
        float scale = size / 16.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.setColor(((tint >> 16) & 0xFF) / 255.0f, ((tint >> 8) & 0xFF) / 255.0f,
                (tint & 0xFF) / 255.0f, ((tint >>> 24) & 0xFF) / 255.0f);
        graphics.blit(location(icon), -8, -8, 0, 0, 16, 16, 16, 16);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }

    public static void invalidate() {
        PRESENT.clear();
    }
}
