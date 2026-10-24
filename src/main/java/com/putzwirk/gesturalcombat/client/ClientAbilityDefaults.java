package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT)
public final class ClientAbilityDefaults {
    private ClientAbilityDefaults() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (AbilityConfigs.loaded()) {
            return;
        }
        AbilityConfigs.ensureLoaded();
        Gesturalcombat.LOGGER.info("gesturalcombat bundled defaults: {} techniques, {} profiles",
                AbilityConfigs.count(), WeaponProfiles.count());
    }
}
