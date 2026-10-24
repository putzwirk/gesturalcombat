package com.putzwirk.gesturalcombat.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class AbilityIcons {
    private static final Map<String, ResourceLocation> LOCATIONS = new HashMap<>();
    private static final Map<String, Boolean> PRESENT = new HashMap<>();

    private AbilityIcons() {
    }

    public static ResourceLocation location(String techniqueId) {
        return LOCATIONS.computeIfAbsent(techniqueId,
                id -> new ResourceLocation("gesturalcombat", "textures/gui/abilities/" + id + ".png"));
    }

    public static boolean present(String techniqueId) {
        return PRESENT.computeIfAbsent(techniqueId,
                id -> Minecraft.getInstance().getResourceManager().getResource(location(id)).isPresent());
    }

    public static void draw(GuiGraphics graphics, String techniqueId, float cx, float cy, float size, int tint) {
        ResourceLocation location = location(techniqueId);
        float scale = size / 16.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.setColor(((tint >> 16) & 0xFF) / 255.0f, ((tint >> 8) & 0xFF) / 255.0f,
                (tint & 0xFF) / 255.0f, ((tint >>> 24) & 0xFF) / 255.0f);
        graphics.blit(location, -8, -8, 0, 0, 16, 16, 16, 16);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }

    public static void invalidate() {
        PRESENT.clear();
    }
}
