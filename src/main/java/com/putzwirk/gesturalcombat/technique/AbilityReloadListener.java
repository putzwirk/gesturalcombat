package com.putzwirk.gesturalcombat.technique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.Optional;

public final class AbilityReloadListener extends SimplePreparableReloadListener<AbilityReloadListener.Payload> {
    private static final ResourceLocation ABILITIES = new ResourceLocation(Gesturalcombat.MODID, "abilities.json");
    private static final ResourceLocation WEAPONS = new ResourceLocation(Gesturalcombat.MODID, "weapons.json");

    public record Payload(JsonObject abilities, JsonObject weapons) {
    }

    @Override
    protected Payload prepare(ResourceManager manager, ProfilerFiller profiler) {
        return new Payload(read(manager, ABILITIES), read(manager, WEAPONS));
    }

    @Override
    protected void apply(Payload payload, ResourceManager manager, ProfilerFiller profiler) {
        if (payload.abilities() == null && payload.weapons() == null) {
            return;
        }
        AbilityConfigs.load(payload.abilities(), payload.weapons());
        Gesturalcombat.LOGGER.info("Loaded ability configs: {} techniques configured, {} weapon profiles",
                AbilityConfigs.count(), com.putzwirk.gesturalcombat.weapon.WeaponProfiles.count());
        AbilitySync.broadcast();
    }

    private static JsonObject read(ResourceManager manager, ResourceLocation id) {
        Optional<Resource> resource = manager.getResource(id);
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            Gesturalcombat.LOGGER.warn("Failed to read {}", id, e);
            return null;
        }
    }
}
