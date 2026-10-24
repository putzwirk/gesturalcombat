package com.putzwirk.gesturalcombat.client;

import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Mod.EventBusSubscriber(modid = Gesturalcombat.MODID, value = Dist.CLIENT)
public final class ClientAbilityDefaults {
    private ClientAbilityDefaults() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (AbilityConfigs.count() > 0 && WeaponProfiles.count() > 0) {
            return;
        }
        String abilities = read("/data/gesturalcombat/abilities.json");
        String weapons = read("/data/gesturalcombat/weapons.json");
        if (abilities == null || weapons == null) {
            Gesturalcombat.LOGGER.warn("Missing bundled ability data");
            return;
        }
        AbilityConfigs.loadFromJson(abilities, weapons);
        Gesturalcombat.LOGGER.info("gesturalcombat bundled defaults: {} techniques, {} profiles",
                AbilityConfigs.count(), WeaponProfiles.count());
    }

    private static String read(String path) {
        try (InputStream stream = ClientAbilityDefaults.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                char[] buffer = new char[8192];
                int read;
                while ((read = reader.read(buffer)) > 0) {
                    builder.append(buffer, 0, read);
                }
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
