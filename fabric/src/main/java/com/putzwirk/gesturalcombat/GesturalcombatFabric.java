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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class GesturalcombatFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Platform.install(new FabricPlatform(), new FabricNetwork());
        Gesturalcombat.init();
        FabricNetwork.registerPayloads();
        FabricNetwork.registerServer();
        registerEvents();
    }

    private static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerRef.set(server);
            WeaponAudit.onServerStarted();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerRef.set(null);
            GestureScheduler.stopped();
            FirePatchManager.stopped();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BleedEffect.tick(server);
            GestureScheduler.tick(server);
            FirePatchManager.tick();
            PostureState.tick(server);
            MomentumState.tick(server);
            States.tick(server);
            WeaponAudit.onServerTick();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AerialSlam.tick(player);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                return ShieldEvents.allowDamage(player, source) && PairedGuard.allowDamage(player, source);
            }
            return true;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            return allowUse(player, hand);
        });
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> WeaponAudit.onRegisterCommands(dispatcher));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> AbilityEvents.onLogin(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> AbilityEvents.onRespawn(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
                (player, origin, destination) -> AbilityEvents.onChangedDimension(player));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                wrap(ResourceLocation.fromNamespaceAndPath(Gesturalcombat.MODID, "abilities"),
                        AbilityEvents.reloadListener()));
    }

    private static InteractionResultHolder<ItemStack> allowUse(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return ShieldEvents.allowUse(player, stack)
                ? InteractionResultHolder.pass(stack) : InteractionResultHolder.fail(stack);
    }

    static IdentifiableResourceReloadListener wrap(ResourceLocation id, PreparableReloadListener delegate) {
        return new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                                  ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                                                  Executor background, Executor game) {
                return delegate.reload(barrier, manager, prepareProfiler, applyProfiler, background, game);
            }
        };
    }
}
