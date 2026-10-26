package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;

public final class ClientAbilityDefaults {
    private ClientAbilityDefaults() {
    }

        public static void onLoggingIn() {
        if (!AbilityConfigs.loaded() && !AbilityConfigs.synced()) {
            AbilityConfigs.ensureLoaded();
            Gesturalcombat.LOGGER.info("gesturalcombat bundled defaults: {} techniques, {} profiles",
                    AbilityConfigs.count(), WeaponProfiles.count());
        }
        com.putzwirk.gesturalcombat.network.ModNetwork.sendToServer(
                new com.putzwirk.gesturalcombat.network.ConfigRequestPacket());
    }
}
