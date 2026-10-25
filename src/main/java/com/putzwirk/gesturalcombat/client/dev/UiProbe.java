package com.putzwirk.gesturalcombat.client.dev;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.client.ClientState;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT)
public final class UiProbe {
    private static final boolean ENABLED = System.getenv("GC_UITEST") != null;
    private static int stage;
    private static int wait;
    private static int warmup;

    private UiProbe() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getOverlay() != null || mc.screen != null) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        switch (stage) {
            case 0 -> {
                if (warmup < 60) {
                    warmup++;
                    return;
                }
                stage = 1;
                wait = 2;
            }
            case 1 -> {
                ClientState.TRAIL.clear();
                ClientState.beginTrail(Archetype.SWORD);
                float centerX = mc.getWindow().getGuiScaledWidth() * 0.5f;
                float centerY = mc.getWindow().getGuiScaledHeight() * 0.5f;
                for (int i = 0; i < 26; i++) {
                    double angle = Math.PI * 2.0 * i / 25.0;
                    ClientState.pushTrailPoint((float) (centerX + Math.cos(angle) * 60.0), (float) (centerY + Math.sin(angle) * 60.0));
                }
                ClientState.stroking = true;
                ClientState.strokeStrength = 74;
                ClientState.shownShape = GestureShape.CIRCLE;
                ClientState.shownConfidence = 0.92f;
                ClientState.strokeTicks = 20;
                stage = 2;
                wait = 2;
            }
            case 2 -> {
                Screenshot.grab(mc.gameDirectory, "gc_hud.png", mc.getMainRenderTarget(), c -> {
                });
                stage = 3;
                Gesturalcombat.LOGGER.info("UITEST_DONE");
            }
            default -> {
            }
        }
    }
}
