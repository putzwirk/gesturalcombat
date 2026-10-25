package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.entity.ModEntities;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.particle.ModParticles;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Gesturalcombat.MODID)
public class Gesturalcombat {
    public static final String MODID = "gesturalcombat";
    public static final Logger LOGGER = LogManager.getLogger();

    public Gesturalcombat() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        ModEntities.ENTITIES.register(modBus);
        ModParticles.TYPES.register(modBus);
        com.putzwirk.gesturalcombat.world.ModTerrain.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                com.putzwirk.gesturalcombat.weapon.WeaponAudit::onRegisterCommands);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                com.putzwirk.gesturalcombat.weapon.WeaponAudit::onServerStarted);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                com.putzwirk.gesturalcombat.weapon.WeaponAudit::onServerTick);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
