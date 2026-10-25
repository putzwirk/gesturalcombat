package com.putzwirk.gesturalcombat.weapon;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.Gesturalcombat;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.technique.AbilityConfigs;
import com.putzwirk.gesturalcombat.technique.GestureScheduler;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import com.putzwirk.gesturalcombat.technique.hitbox.Hitbox;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WeaponAudit {
    public static final String TAG = "weapon check:";
    public static final String GAP = "weapon check gap:";

    private static final int HIT_WINDOW = 80;
    private static final double PROXIMITY = 8.0;
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Pending(String technique, String item, int hits) {
    }

    public record Slot(GestureShape shape, String technique) {
    }

    public record Gap(String kind, String detail) {
        public boolean reported() {
            return switch (kind) {
                case "no-profile", "allowed-off" -> false;
                default -> true;
            };
        }
    }

    public record Report(String item, Archetype archetype, String family, double reach, boolean profile,
                         List<Slot> slots, List<Gap> gaps) {
    }

    private WeaponAudit() {
    }

    public static List<Report> scan() {
        AbilityConfigs.ensureLoaded();
        List<Report> reports = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ItemStack stack = new ItemStack(item);
            Archetype archetype = Archetype.of(stack);
            if (archetype == null) {
                continue;
            }
            String id = idOf(item);
            String family = WeaponProfiles.family(stack);
            WeaponProfile profile = WeaponProfiles.profile(stack);
            List<Slot> slots = new ArrayList<>();
            List<Gap> gaps = new ArrayList<>();
            Set<String> used = new LinkedHashSet<>();
            for (GestureShape shape : GestureShape.STRIP) {
                Technique resolved = Techniques.available(archetype, shape, stack);
                if (resolved != null) {
                    slots.add(new Slot(shape, resolved.id()));
                    if (!used.add(resolved.id())) {
                        gaps.add(new Gap("duplicate", resolved.id() + " is reachable from more than one slot"));
                    }
                    if (AbilityConfigs.of(resolved) == null) {
                        gaps.add(new Gap("no-tuning", resolved.id() + " (" + shape + ") has no entry in abilities.json"));
                    }
                    Hitbox hitbox = AbilityConfigs.hitbox(resolved, stack, Hitbox.arc(2.5, 90.0));
                    if (hitbox.maxTargets() <= 0 || hitbox.range() < 0.8) {
                        gaps.add(new Gap("no-reach", resolved.id() + " (" + shape + ") cannot reach a target"));
                    }
                    continue;
                }
                Technique declared = family == null ? null : Techniques.familyTechnique(family, shape);
                if (declared == null) {
                    continue;
                }
                if (WeaponRules.blocked(stack, shape)) {
                    gaps.add(new Gap("unreachable", declared.id() + " (" + shape + ") is defined but " + family
                            + " locks that gesture"));
                } else if (!WeaponProfiles.allows(stack, shape)) {
                    gaps.add(new Gap("allowed-off", declared.id() + " (" + shape + ") is hidden by the gesture allow-list"));
                } else if (declared.archetype() != archetype) {
                    gaps.add(new Gap("archetype", declared.id() + " is " + declared.archetype() + " ("
                            + declared.shape() + ") but the item is " + archetype + " " + shape));
                } else {
                    gaps.add(new Gap("unresolved", declared.id() + " (" + shape + ") is defined but does not resolve"));
                }
            }
            if (profile == null) {
                gaps.add(new Gap("no-profile", "no profile under weapons/, falling back to the " + archetype.id() + " kit"));
            }
            reports.add(new Report(id, archetype, family, WeaponProfiles.reachMultiplier(stack), profile != null, slots, gaps));
        }
        reports.sort((a, b) -> a.item().compareTo(b.item()));
        return reports;
    }

    public static String summary() {
        StringBuilder text = new StringBuilder();
        for (Report report : scan()) {
            text.append(report.item()).append('=')
                    .append(report.archetype().name()).append('|')
                    .append(report.family() == null ? "-" : report.family()).append('|');
            for (int i = 0; i < report.slots().size(); i++) {
                Slot slot = report.slots().get(i);
                if (i > 0) {
                    text.append(',');
                }
                text.append(slot.shape().name()).append(':').append(slot.technique());
            }
            text.append('\n');
        }
        return text.toString();
    }

    public static void log(String origin) {
        List<Report> reports = scan();
        int reported = 0;
        for (Report report : reports) {
            int gaps = 0;
            for (Gap gap : report.gaps()) {
                if (gap.reported()) {
                    gaps++;
                }
            }
            reported += gaps;
            Gesturalcombat.LOGGER.info("{} [{}] {} arch={} family={} reach={}x profile={} slots={} [{}] gaps={}",
                    TAG, origin, report.item(), report.archetype().id(), report.family() == null ? "-" : report.family(),
                    report.reach(), report.profile() ? "yes" : "no", report.slots().size(), slotText(report), gaps);
            for (Gap gap : report.gaps()) {
                if (gap.reported()) {
                    Gesturalcombat.LOGGER.warn("{} [{}] {} [{}] {}", GAP, origin, report.item(), gap.kind(), gap.detail());
                } else {
                    Gesturalcombat.LOGGER.info("{} [{}] {} [{}] {}", GAP, origin, report.item(), gap.kind(), gap.detail());
                }
            }
        }
        Gesturalcombat.LOGGER.info("{} [{}] {} weapons scanned, {} gaps reported", TAG, origin, reports.size(), reported);
    }

    public static void compare(String serverSummary, String origin) {
        compare(serverSummary, scan(), origin);
    }

    public static void compare(String serverSummary, List<Report> reports, String origin) {
        Map<String, String> remote = parse(serverSummary);
        Map<String, String> local = new LinkedHashMap<>();
        for (Report report : reports) {
            local.put(report.item(), signature(report));
        }
        int mismatches = 0;
        for (Map.Entry<String, String> entry : remote.entrySet()) {
            String mine = local.get(entry.getKey());
            if (mine == null) {
                mismatches++;
                Gesturalcombat.LOGGER.warn("{} [{}] {} is missing on this side (server={})",
                        GAP, origin, entry.getKey(), entry.getValue());
            } else if (!mine.equals(entry.getValue())) {
                mismatches++;
                Gesturalcombat.LOGGER.warn("{} [{}] {} server={} local={}",
                        GAP, origin, entry.getKey(), entry.getValue(), mine);
            }
        }
        for (String item : local.keySet()) {
            if (!remote.containsKey(item)) {
                mismatches++;
                Gesturalcombat.LOGGER.warn("{} [{}] {} is missing on the server (local={})",
                        GAP, origin, item, local.get(item));
            }
        }
        if (mismatches == 0) {
            Gesturalcombat.LOGGER.info("{} [{}] client and server resolve the same slots for {} weapons",
                    TAG, origin, local.size());
        } else {
            Gesturalcombat.LOGGER.warn("{} [{}] {} weapon(s) resolve differently across the two sides",
                    TAG, origin, mismatches);
        }
    }

    public static void beginCast(ServerPlayer player, Technique technique, ItemStack stack) {
        if (!Config.INSTANCE.debugWeapons.get() || player.level().isClientSide()) {
            return;
        }
        if (!expectsHit(technique, stack)) {
            return;
        }
        Level level = player.level();
        if (!targetNearby(player, level)) {
            return;
        }
        UUID id = player.getUUID();
        PENDING.put(id, new Pending(technique.id(), idOf(stack.getItem()), 0));
        GestureScheduler.schedule(level, HIT_WINDOW, () -> finish(player, id));
    }

    public static void noteHit(ServerPlayer player, Technique technique) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.technique().equals(technique.id())) {
            return;
        }
        PENDING.put(player.getUUID(), new Pending(pending.technique(), pending.item(), pending.hits() + 1));
    }

    private static void finish(ServerPlayer player, UUID id) {
        Pending pending = PENDING.remove(id);
        if (pending == null) {
            return;
        }
        if (pending.hits() > 0) {
            Gesturalcombat.LOGGER.info("{} hit {} {} landed on {} target(s)", TAG, pending.item(), pending.technique(), pending.hits());
            return;
        }
        Gesturalcombat.LOGGER.warn("{} {} {} cast with a target within {} blocks but hit nothing in {} ticks",
                GAP, pending.item(), pending.technique(), (int) PROXIMITY, HIT_WINDOW);
    }

    private static boolean expectsHit(Technique technique, ItemStack stack) {
        if (technique.shape() == GestureShape.V || technique.archetype() == Archetype.BOW) {
            return false;
        }
        Hitbox hitbox = AbilityConfigs.hitbox(technique, stack, Hitbox.arc(2.5, 90.0));
        return hitbox.maxTargets() > 0 && AbilityConfigs.damageMultiplier(technique, stack, 1.0) > 0.0f;
    }

    private static boolean targetNearby(ServerPlayer player, Level level) {
        return !level.getEntities(player, player.getBoundingBox().inflate(PROXIMITY),
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity != player).isEmpty();
    }

    private static String slotText(Report report) {
        StringBuilder text = new StringBuilder();
        for (Slot slot : report.slots()) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(slot.shape().id()).append('=').append(slot.technique());
        }
        return text.toString();
    }

    private static String signature(Report report) {
        StringBuilder text = new StringBuilder();
        text.append(report.archetype().name()).append('|')
                .append(report.family() == null ? "-" : report.family()).append('|');
        for (int i = 0; i < report.slots().size(); i++) {
            Slot slot = report.slots().get(i);
            if (i > 0) {
                text.append(',');
            }
            text.append(slot.shape().name()).append(':').append(slot.technique());
        }
        return text.toString();
    }

    private static Map<String, String> parse(String summary) {
        Map<String, String> parsed = new LinkedHashMap<>();
        if (summary == null || summary.isBlank()) {
            return parsed;
        }
        for (String line : summary.split("\n")) {
            int split = line.indexOf('=');
            if (split > 0) {
                parsed.put(line.substring(0, split), line.substring(split + 1));
            }
        }
        return parsed;
    }

    public static String idOf(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "unknown" : key.toString().toLowerCase(Locale.ROOT);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!Config.INSTANCE.debugWeapons.get()) {
            return;
        }
        log("startup");
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("gesturalcombat")
                .then(Commands.literal("audit")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            log("command");
                            com.putzwirk.gesturalcombat.technique.AbilitySync.broadcast();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("gesturalcombat: audited every weapon, see the log"), false);
                            return 1;
                        })));
    }
}
