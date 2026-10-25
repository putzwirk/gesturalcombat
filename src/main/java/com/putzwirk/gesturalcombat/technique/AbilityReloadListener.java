package com.putzwirk.gesturalcombat.technique;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AbilityReloadListener extends SimplePreparableReloadListener<AbilityReloadListener.Payload> {
    private static final ResourceLocation ABILITIES = new ResourceLocation(Gesturalcombat.MODID, "abilities.json");
    private static final String WEAPONS_DIR = "weapons";

    public record Payload(JsonObject abilities, JsonObject weapons) {
    }

    @Override
    protected Payload prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<JsonObject> weaponDocs = new ArrayList<>();
        Map<ResourceLocation, Resource> found = manager.listResources(WEAPONS_DIR,
                id -> id.getNamespace().equals(Gesturalcombat.MODID) && id.getPath().endsWith(".json"));
        found.keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::getPath))
                .forEach(id -> {
                    JsonObject doc = read(manager, id);
                    if (doc != null) {
                        weaponDocs.add(doc);
                    }
                });
        JsonObject abilities = read(manager, ABILITIES);
        if (abilities == null) {
            abilities = AbilityConfigs.bundledAbilities();
        }
        List<JsonObject> docs = weaponDocs.isEmpty() ? AbilityConfigs.bundledWeaponDocs() : weaponDocs;
        return new Payload(abilities, AbilityConfigs.mergeWeapons(docs));
    }

    @Override
    protected void apply(Payload payload, ResourceManager manager, ProfilerFiller profiler) {
        if (payload.abilities() == null && payload.weapons() == null) {
            return;
        }
        AbilityConfigs.load(payload.abilities(), payload.weapons());
        Gesturalcombat.LOGGER.info("Loaded ability configs: {} techniques configured, {} weapon profiles",
                AbilityConfigs.count(), com.putzwirk.gesturalcombat.weapon.WeaponProfiles.count());
        if (com.putzwirk.gesturalcombat.Config.INSTANCE.debugWeapons.get()) {
            com.putzwirk.gesturalcombat.weapon.WeaponAudit.requestAudit("reload");
        }
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
