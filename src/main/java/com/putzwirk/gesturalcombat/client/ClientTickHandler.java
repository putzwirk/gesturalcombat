package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.client.anim.AnimationScheduler;
import com.putzwirk.gesturalcombat.client.gesture.GestureInput;
import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT)
public final class ClientTickHandler {
    private static Screen lastScreen;

    private ClientTickHandler() {
    }

    private static void keepCursorVisible(Minecraft mc) {
        if (mc.screen == lastScreen) {
            return;
        }
        lastScreen = mc.screen;
        if (mc.screen == null) {
            return;
        }
        mc.mouseHandler.releaseMouse();
        long handle = mc.getWindow().getWindow();
        if (handle != 0L) {
            GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientState.ticks++;
        Minecraft mc = Minecraft.getInstance();
        keepCursorVisible(mc);
        if (ClientState.flashTicks > 0) {
            ClientState.flashTicks--;
        }
        if (ClientState.castTicks > 0) {
            ClientState.castTicks--;
        }
        if (ClientState.rejectTicks > 0) {
            ClientState.rejectTicks--;
        }
        if (ClientState.guardTicks > 0) {
            ClientState.guardTicks--;
        }
        if (ClientState.shieldLockTicks > 0) {
            ClientState.shieldLockTicks--;
        }
        if (mc.player == null || mc.level == null) {
            ClientState.reset();
            return;
        }
        AnimationScheduler.tick();
        com.putzwirk.gesturalcombat.client.fx.TrailManager.tick();
        GestureInput.tick(mc);
        if (GestureInput.suppressVanillaAttack()) {
            BetterCombatBridge.cancelPendingSwing(mc);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        AnimationScheduler.clear();
        ClientState.reset();
        com.putzwirk.gesturalcombat.client.render.HitboxDebug.clear();
        com.putzwirk.gesturalcombat.client.fx.TrailManager.clear();
        com.putzwirk.gesturalcombat.technique.AbilityConfigs.clear();
        com.putzwirk.gesturalcombat.technique.Techniques.applyConfig();
    }
}
