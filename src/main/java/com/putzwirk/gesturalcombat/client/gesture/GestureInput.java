package com.putzwirk.gesturalcombat.client.gesture;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.client.ClientState;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.core.HandGate;
import com.putzwirk.gesturalcombat.compat.BetterCombatBridge;
import com.putzwirk.gesturalcombat.network.CastTechniquePacket;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.technique.GuardState;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import com.putzwirk.gesturalcombat.weapon.CooldownKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GestureInput {
    private static final float ENGAGE_LENGTH = 0.032f;
    private static final float MIN_STROKE_LENGTH = 0.035f;
    private static final float MIN_CONFIDENCE = 0.40f;
    private static final int MIN_SAMPLES = 4;
    private static final float MIN_SAMPLE_LENGTH = 1.0e-4f;
    private static final float SAMPLE_SPACING = 1.5f;
    private static final float STRENGTH_LENGTH = 0.6f;
    private static final int LIVE_PERIOD = 2;
    private static final int REJECT_TICKS = 8;
    private static final int CAST_TICKS = 8;
    private static final int TRAIL_MAX_AGE = 18;
    private static final int CAST_BUFFER_TICKS = 6;
    private static final float TRAIL_SENSITIVITY = 0.42f;

    private static final List<float[]> POINTS = new ArrayList<>();
    private static final Map<Item, Boolean> USE_OVERRIDE = new HashMap<>();

    private static boolean active;
    private static boolean engaged;
    private static boolean attackWasDown;
    private static boolean useWasDown;
    private static InteractionHand strokeHand;
    private static boolean consumeRelease;
    private static int strokeAge;
    private static float cursorX;
    private static float cursorY;
    private static float pendingX;
    private static float pendingY;
    private static float pathLength;
    private static int guiWidth = 1;
    private static int guiHeight = 1;
    private static Archetype strokeArchetype;
    private static double rawX;
    private static double rawY;
    private static boolean rawReady;
    private static PendingCast pending;
    private static float trailOriginX;
    private static float trailOriginY;

    private GestureInput() {
    }

    public static void tick(Minecraft mc) {
        ClientState.pruneTrail(TRAIL_MAX_AGE);
        tickPending(mc);
        if (mc.player == null || mc.level == null) {
            resetStroke();
            attackWasDown = false;
            useWasDown = false;
            return;
        }

        boolean attackDown = mc.options.keyAttack.isDown();
        boolean useDown = mc.options.keyUse.isDown();
        Archetype mainArchetype = Archetype.of(mc.player.getMainHandItem());
        Archetype offArchetype = Archetype.of(mc.player.getOffhandItem());
        boolean baseAllowed = mc.screen == null
                && mc.mouseHandler.isMouseGrabbed()
                && Config.INSTANCE.gesturesEnabled.get();
        boolean mining = canMineWithWeapon(mc);
        boolean mainAllowed = baseAllowed && mainArchetype != null && bowReady(mc, mainArchetype);
        boolean mainFood = HandGate.blocksOffhand(mc.player.getMainHandItem());
        boolean offAllowed = baseAllowed
                && !mainFood
                && mainArchetype != Archetype.BOW
                && offArchetype != null
                && bowReady(mc, offArchetype);

        if (!baseAllowed) {
            if (active) {
                resetStroke();
            }
            attackWasDown = attackDown;
            useWasDown = useDown;
            return;
        }

        if (active && (Archetype.forHand(mc.player, strokeHand) != strokeArchetype
                || (strokeHand == InteractionHand.OFF_HAND && mainFood))) {
            resetStroke();
        }

        if (!active) {
            boolean attackPressed = attackDown && !attackWasDown;
            boolean usePressed = useDown && !useWasDown;
            if (useDown && mainArchetype == Archetype.BOW && mainAllowed) {
                begin(mc, mainArchetype, InteractionHand.MAIN_HAND);
            } else if (usePressed && offArchetype == Archetype.BOW && offAllowed) {
                begin(mc, offArchetype, InteractionHand.OFF_HAND);
            } else if (attackPressed && !mining && mainArchetype != Archetype.BOW && mainAllowed) {
                begin(mc, mainArchetype, InteractionHand.MAIN_HAND);
            } else if (usePressed && offArchetype != Archetype.BOW && offAllowed) {
                begin(mc, offArchetype, InteractionHand.OFF_HAND);
            }
        } else if (strokeArchetype == Archetype.BOW) {
            if (!useDown) {
                resetStroke();
            }
        } else if (strokeHand == InteractionHand.MAIN_HAND && !attackDown) {
            finish(mc);
        } else if (strokeHand == InteractionHand.OFF_HAND && !useDown) {
            finish(mc);
        }
        attackWasDown = attackDown;
        useWasDown = useDown;

        if (!active) {
            return;
        }

        strokeAge++;
        ClientState.strokeTicks = strokeAge;
        ClientState.strokeStrength = engaged ? strength() : 0;
        if (strokeAge % LIVE_PERIOD == 0 && engaged && POINTS.size() >= 2) {
            GestureRecognizer.Result result = GestureRecognizer.recognize(POINTS);
            ClientState.shownShape = result.shape();
            ClientState.shownConfidence = result.confidence();
        }
    }

    public static boolean isDraggingGesture() {
        return engaged;
    }

    public static Archetype strokeArchetype() {
        return active ? strokeArchetype : null;
    }

    public static InteractionHand strokeHand() {
        return active ? strokeHand : null;
    }

    public static boolean consumeVanillaRelease() {
        return consumeRelease;
    }

    public static boolean finishBowRelease() {
        Minecraft mc = Minecraft.getInstance();
        if (!active || strokeHand != InteractionHand.MAIN_HAND || strokeArchetype != Archetype.BOW) {
            return false;
        }
        if (mc.player == null) {
            resetStroke();
            return false;
        }
        boolean cast = finish(mc);
        if (!cast) {
            resetStroke();
            return false;
        }
        consumeRelease = true;
        mc.player.stopUsingItem();
        consumeRelease = false;
        return true;
    }

    private static boolean bowReady(Minecraft mc, Archetype archetype) {
        if (archetype != Archetype.BOW) {
            return true;
        }
        return mc.player.isUsingItem()
                && mc.player.getUseItem().getItem() instanceof BowItem
                && mc.player.getTicksUsingItem() >= 20;
    }

    public static void onRawMouseMove(double x, double y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || !mc.mouseHandler.isMouseGrabbed()) {
            rawReady = false;
            return;
        }
        if (!rawReady) {
            rawX = x;
            rawY = y;
            rawReady = true;
            return;
        }
        double dx = x - rawX;
        double dy = y - rawY;
        rawX = x;
        rawY = y;
        onMouseDelta(dx, dy);
    }

    public static void onMouseDelta(double dx, double dy) {
        if (!active) {
            return;
        }
        if (dx == 0.0 && dy == 0.0) {
            return;
        }
        float nx = (float) (dx / guiWidth);
        float ny = (float) (-dy / guiHeight);
        if (nx * nx + ny * ny < MIN_SAMPLE_LENGTH * MIN_SAMPLE_LENGTH) {
            return;
        }
        pendingX += nx;
        pendingY += ny;
        float px = pendingX * guiWidth;
        float py = pendingY * guiHeight;
        if (px * px + py * py < SAMPLE_SPACING * SAMPLE_SPACING) {
            return;
        }
        addPoint(cursorX + pendingX, cursorY + pendingY);
        pendingX = 0.0f;
        pendingY = 0.0f;
    }

    private static void tickPending(Minecraft mc) {
        if (pending == null) {
            ClientState.pendingTicks = 0;
            return;
        }
        if (mc.player == null || mc.level == null || mc.screen != null) {
            clearPending();
            return;
        }
        Technique technique = Techniques.get(pending.id());
        if (technique == null
                || (pending.hand() == InteractionHand.OFF_HAND && HandGate.blocksOffhand(mc.player.getMainHandItem()))
                || (technique.archetype() == Archetype.SHIELD && ClientState.shieldLockTicks > 0)
                || Archetype.forHand(mc.player, pending.hand()) != technique.archetype()) {
            clearPending();
            return;
        }
        int remaining = ClientState.cooldownRemaining(pending.cooldownKey());
        ClientState.pendingTicks = remaining;
        if (remaining <= 0) {
            PendingCast cast = pending;
            clearPending();
            if (ClientState.MASTERY.isTechniqueAvailable(technique) && ClientState.isReady(cast.cooldownKey())) {
                fire(technique, cast.shape(), cast.confidence(), cast.swipe(), cast.hand());
            }
        }
    }

    private static void clearPending() {
        pending = null;
        ClientState.pendingTechnique = "";
        ClientState.pendingTicks = 0;
    }

    public static boolean suppressVanillaAttack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            return false;
        }
        if (!Config.INSTANCE.gesturesEnabled.get()) {
            return false;
        }
        if (canMineWithWeapon(mc)) {
            return false;
        }
        return Archetype.of(mc.player.getMainHandItem()) != null;
    }

    public static boolean canMineWithWeapon(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return false;
        }
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (!BetterCombatBridge.isMiningWithWeaponsEnabled()) {
            return false;
        }
        Archetype held = Archetype.of(mc.player.getMainHandItem());
        if (held != Archetype.SWORD && held != Archetype.AXE) {
            return false;
        }
        BlockState state = mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos());
        if (held == Archetype.SWORD) {
            return state.is(BlockTags.SWORD_EFFICIENT) || state.is(BlockTags.BAMBOO_BLOCKS);
        }
        return state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    public static boolean suppressVanillaUse() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            return false;
        }
        if (!Config.INSTANCE.gesturesEnabled.get()) {
            return false;
        }
        if (!engaged || strokeHand != InteractionHand.OFF_HAND) {
            return false;
        }
        return !mainHandUses(mc);
    }

    private static boolean mainHandUses(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        ItemStack main = mc.player.getMainHandItem();
        if (main.isEmpty()) {
            return false;
        }
        return USE_OVERRIDE.computeIfAbsent(main.getItem(), item -> {
            Method base = findUse(Item.class);
            Method actual = findUse(item.getClass());
            return base != null && actual != null && !base.equals(actual);
        });
    }

    private static Method findUse(Class<?> type) {
        for (Method method : type.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 3
                    && parameters[0] == Level.class
                    && parameters[1] == Player.class
                    && parameters[2] == InteractionHand.class
                    && method.getReturnType() == InteractionResultHolder.class) {
                return method;
            }
        }
        return null;
    }

    private static void begin(Minecraft mc, Archetype archetype, InteractionHand hand) {
        clearPending();
        active = true;
        engaged = false;
        strokeHand = hand;
        POINTS.clear();
        strokeAge = 0;
        pathLength = 0.0f;
        pendingX = 0.0f;
        pendingY = 0.0f;
        cursorX = 0.0f;
        cursorY = 0.0f;
        guiWidth = Math.max(1, mc.getWindow().getGuiScaledWidth());
        guiHeight = Math.max(1, mc.getWindow().getGuiScaledHeight());
        strokeArchetype = archetype;
        ClientState.stroking = false;
        ClientState.strokeTicks = 0;
        ClientState.strokeStrength = 0;
        ClientState.shownShape = GestureShape.UNKNOWN;
        ClientState.shownConfidence = 0.0f;
        POINTS.add(new float[]{0.0f, 0.0f});
        trailOriginX = guiWidth * 0.5f;
        trailOriginY = guiHeight * 0.5f;
        ClientState.beginTrail(archetype);
        ClientState.pushTrailPoint(trailOriginX, trailOriginY);
    }

    private static void addPoint(float x, float y) {
        float dx = x - cursorX;
        float dy = y - cursorY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < MIN_SAMPLE_LENGTH) {
            return;
        }
        if (POINTS.size() < 4096) {
            POINTS.add(new float[]{x, y});
        }
        pathLength += length;
        cursorX = x;
        cursorY = y;
        ClientState.pushTrailPoint(trailOriginX + x * guiWidth * TRAIL_SENSITIVITY, trailOriginY - y * guiHeight * TRAIL_SENSITIVITY);
        if (!engaged && pathLength >= ENGAGE_LENGTH) {
            engaged = true;
            ClientState.stroking = true;
            if (strokeArchetype == Archetype.BOW && strokeHand == InteractionHand.OFF_HAND) {
                consumeRelease = true;
            }
        }
    }

    private static boolean finish(Minecraft mc) {
        boolean wasEngaged = engaged;
        InteractionHand hand = strokeHand;
        active = false;
        engaged = false;
        ClientState.stroking = false;
        ClientState.strokeStrength = 0;
        if (!wasEngaged) {
            POINTS.clear();
            ClientState.shownShape = GestureShape.UNKNOWN;
            ClientState.shownConfidence = 0.0f;
            strokeAge = 0;
            strokeArchetype = null;
            return false;
        }
        if (pathLength < MIN_STROKE_LENGTH || POINTS.size() < MIN_SAMPLES) {
            POINTS.clear();
            strokeAge = 0;
            strokeArchetype = null;
            return false;
        }
        GestureRecognizer.Result result = GestureRecognizer.recognize(POINTS);
        int swipe = swipeDirection();
        ClientState.shownShape = result.shape();
        ClientState.shownConfidence = result.confidence();
        Archetype archetype = strokeArchetype;
        POINTS.clear();
        strokeAge = 0;
        strokeArchetype = null;
        if (result.shape() == GestureShape.UNKNOWN || result.confidence() < MIN_CONFIDENCE) {
            return false;
        }
        if (archetype == Archetype.SHIELD && ClientState.shieldLockTicks > 0) {
            ClientState.rejectTicks = REJECT_TICKS;
            return false;
        }
        ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getItemInHand(hand);
        Technique technique = Techniques.forGesture(archetype, result.shape(), held);
        if (technique == null || !ClientState.MASTERY.isTechniqueAvailable(technique)) {
            ClientState.rejectTicks = REJECT_TICKS;
            return false;
        }
        String cooldownKey = CooldownKeys.of(held, result.shape(), technique);
        int remaining = ClientState.cooldownRemaining(cooldownKey);
        if (remaining > 0) {
            if (remaining <= CAST_BUFFER_TICKS) {
                pending = new PendingCast(technique.id(), result.shape(), result.confidence(), swipe, hand, cooldownKey);
                ClientState.pendingTechnique = technique.id();
                ClientState.pendingTicks = remaining;
                return true;
            }
            ClientState.rejectTicks = REJECT_TICKS;
            return false;
        }
        fire(technique, result.shape(), result.confidence(), swipe, hand);
        return true;
    }

    private static void fire(Technique technique, GestureShape shape, float confidence, int swipe, InteractionHand hand) {
        clearPending();
        Minecraft mc = Minecraft.getInstance();
        if (ClientState.guardTicks > 0 && mc.player != null) {
            ClientState.guardTicks = 0;
            GuardState.end(mc.player);
            if (mc.player.isUsingItem()) {
                mc.player.stopUsingItem();
            }
        }
        ModNetwork.sendToServer(new CastTechniquePacket(technique.id(), shape, confidence, swipe, hand == InteractionHand.OFF_HAND));
        ClientState.lastTechnique = technique.id();
        ClientState.castTicks = CAST_TICKS;
    }

    private record PendingCast(String id, GestureShape shape, float confidence, int swipe, InteractionHand hand, String cooldownKey) {
    }

    private static int swipeDirection() {
        if (POINTS.size() < 2) {
            return 0;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (float[] point : POINTS) {
            minX = Math.min(minX, point[0]);
            maxX = Math.max(maxX, point[0]);
        }
        float width = maxX - minX;
        float dx = POINTS.get(POINTS.size() - 1)[0] - POINTS.get(0)[0];
        if (width < 1.0e-4f || Math.abs(dx) < width * 0.35f) {
            return 0;
        }
        return dx > 0.0f ? 1 : -1;
    }

    private static void resetStroke() {
        active = false;
        engaged = false;
        consumeRelease = false;
        strokeHand = null;
        POINTS.clear();
        strokeAge = 0;
        pendingX = 0.0f;
        pendingY = 0.0f;
        pathLength = 0.0f;
        strokeArchetype = null;
        ClientState.stroking = false;
        ClientState.strokeTicks = 0;
        ClientState.strokeStrength = 0;
        ClientState.shownShape = GestureShape.UNKNOWN;
        ClientState.shownConfidence = 0.0f;
    }

    private static int strength() {
        return Math.min(100, Math.round(pathLength / STRENGTH_LENGTH * 100.0f));
    }
}
