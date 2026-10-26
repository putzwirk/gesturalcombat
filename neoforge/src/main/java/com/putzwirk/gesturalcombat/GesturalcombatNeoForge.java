package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.platform.Platform;
import com.putzwirk.gesturalcombat.platform.ServerRef;
import com.putzwirk.gesturalcombat.server.AerialSlam;
import com.putzwirk.gesturalcombat.technique.AbilityEvents;
import com.putzwirk.gesturalcombat.technique.FirePatchManager;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.MomentumState;
import com.putzwirk.gesturalcombat.technique.PairedGuard;
import com.putzwirk.gesturalcombat.technique.PostureState;
import com.putzwirk.gesturalcombat.technique.ShieldEvents;
import com.putzwirk.gesturalcombat.technique.States;
import com.putzwirk.gesturalcombat.technique.effect.BleedEffect;
import com.putzwirk.gesturalcombat.weapon.WeaponAudit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Gesturalcombat.MODID)
public final class GesturalcombatNeoForge {
    public GesturalcombatNeoForge(IEventBus modBus, ModContainer container) {
        NeoForgePlatform platform = new NeoForgePlatform();
        Platform.install(platform, new NeoForgeNetwork());
        Gesturalcombat.init();
        platform.attach(modBus);
        modBus.addListener(NeoForgeNetwork::register);
        NeoForge.EVENT_BUS.register(new GameEvents());
    }

    private static final class GameEvents {
        @SubscribeEvent
        public void serverStarted(ServerStartedEvent event) {
            ServerRef.set(event.getServer());
            WeaponAudit.onServerStarted();
        }

        @SubscribeEvent
        public void serverStopped(ServerStoppedEvent event) {
            ServerRef.set(null);
            GestureScheduler.stopped();
            FirePatchManager.stopped();
        }

        @SubscribeEvent
        public void serverTick(ServerTickEvent.Post event) {
            BleedEffect.tick(event.getServer());
            GestureScheduler.tick(event.getServer());
            FirePatchManager.tick();
            PostureState.tick(event.getServer());
            MomentumState.tick(event.getServer());
            States.tick(event.getServer());
            WeaponAudit.onServerTick();
        }

        @SubscribeEvent
        public void playerTick(PlayerTickEvent.Post event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AerialSlam.tick(player);
            }
        }

        @SubscribeEvent
        public void incomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (!ShieldEvents.allowDamage(player, event.getSource())
                    || !PairedGuard.allowDamage(player, event.getSource())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void useItem(LivingEntityUseItemEvent.Start event) {
            if (event.getEntity() instanceof Player player && !ShieldEvents.allowUse(player, event.getItem())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void addReloadListener(AddReloadListenerEvent event) {
            event.addListener(AbilityEvents.reloadListener());
        }

        @SubscribeEvent
        public void login(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AbilityEvents.onLogin(player);
            }
        }

        @SubscribeEvent
        public void respawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AbilityEvents.onRespawn(player);
            }
        }

        @SubscribeEvent
        public void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AbilityEvents.onChangedDimension(player);
            }
        }

        @SubscribeEvent
        public void registerCommands(RegisterCommandsEvent event) {
            WeaponAudit.onRegisterCommands(event.getDispatcher());
        }
    }
}
