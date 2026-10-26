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
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;

@Mod(Gesturalcombat.MODID)
public final class GesturalcombatForge {
    private final ForgePlatform platform = new ForgePlatform();

    public GesturalcombatForge() {
        Platform.install(platform, new ForgeNetwork());
        Gesturalcombat.init();
        platform.attach(FMLJavaModLoadingContext.get().getModEventBus());
        ForgeNetwork.register();
        MinecraftForge.EVENT_BUS.register(new ForgeGameEvents());
    }

    private static final class ForgeGameEvents {
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
        public void serverTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            BleedEffect.tick(event.getServer());
            GestureScheduler.tick(event.getServer());
            FirePatchManager.tick();
            PostureState.tick(event.getServer());
            MomentumState.tick(event.getServer());
            States.tick(event.getServer());
            WeaponAudit.onServerTick();
        }

        @SubscribeEvent
        public void playerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
                AerialSlam.tick(player);
            }
        }

        @SubscribeEvent
        public void livingAttack(LivingAttackEvent event) {
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
