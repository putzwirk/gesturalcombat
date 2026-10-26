package com.putzwirk.gesturalcombat;

import com.putzwirk.gesturalcombat.entity.ModEntities;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.particle.ModParticles;
import com.putzwirk.gesturalcombat.world.ModTerrain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Gesturalcombat {
    public static final String MODID = "gesturalcombat";
    public static final Logger LOGGER = LogManager.getLogger();

    private Gesturalcombat() {
    }

    public static void init() {
        Config.load();
        ModEntities.registerAll();
        ModParticles.registerAll();
        ModTerrain.registerAll();
        ModNetwork.registerAll();
    }
}
