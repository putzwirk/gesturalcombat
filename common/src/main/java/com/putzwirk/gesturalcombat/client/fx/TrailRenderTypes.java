package com.putzwirk.gesturalcombat.client.fx;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class TrailRenderTypes {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(Gesturalcombat.MODID, "textures/particle/trail.png");

    public static final RenderType TRAIL = RenderType.entityTranslucentEmissive(TEXTURE);

    private TrailRenderTypes() {
    }
}
