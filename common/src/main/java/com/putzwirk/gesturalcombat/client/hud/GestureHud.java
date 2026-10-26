package com.putzwirk.gesturalcombat.client.hud;

import com.putzwirk.gesturalcombat.client.ClientState;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.core.GestureShape;
import com.putzwirk.gesturalcombat.core.HandGate;
import com.putzwirk.gesturalcombat.technique.Technique;
import com.putzwirk.gesturalcombat.technique.Techniques;
import com.putzwirk.gesturalcombat.client.gesture.GestureInput;
import com.putzwirk.gesturalcombat.weapon.CooldownKeys;
import com.putzwirk.gesturalcombat.weapon.WeaponProfiles;
import com.putzwirk.gesturalcombat.weapon.WeaponSpecial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class GestureHud {
    private static final int TRAIL_LIFE = 18;
    private static final GestureShape[] STRIP = GestureShape.STRIP;
    private static final float GLYPH_SIZE = 16.0f;
    private static final float SLOT_SPACING = 21.0f;
    private static final float PLATE_SIZE = 19.0f;
    private static final int READY_FLASH = 10;
    private static final float COOLING_ALPHA = 0.80f;
    private static final int PLATE = 0x73000000;
    private static final int PLATE_EMPTY = 0x40000000;
    private static final int LOCKED = 0x33FFFFFF;
    private static final int LOCKED_ICON = 0xFF6E6E78;
    private static final int REJECT = 0xFFFF5A4A;
    private static final int WOUND_GLOW = 0xFF4A0104;
    private static final int WOUND_DEEP = 0xFF240002;
    private static final int WOUND_BODY = 0xFF5C0307;
    private static final int WOUND_WET = 0xFF8A0A0F;
    private static final int WOUND_BRIGHT = 0xFFC22A22;
    private static final int WOUND_CRUST = 0xFF140001;

    private static final java.util.Map<String, Integer> READY_AT = new java.util.HashMap<>();
    private static final java.util.Map<String, Float> LAST_FRACTION = new java.util.HashMap<>();

    private GestureHud() {
    }

    private static Archetype heldArchetype() {
        if (ClientState.stroking) {
            Archetype stroke = GestureInput.strokeArchetype();
            if (stroke != null) {
                return stroke;
            }
        }
        return ClientState.currentArchetype();
    }

    public static void render(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }
        Archetype held = heldArchetype();
        ItemStack mainStack = mc.player.getMainHandItem();
        ItemStack offStack = mc.player.getOffhandItem();
        Archetype mainHand = Archetype.of(mainStack);
        Archetype offHand = HandGate.blocksOffhand(mainStack) ? null : Archetype.of(offStack);
        boolean stroking = ClientState.stroking;
        InteractionHand strokeHand = GestureInput.strokeHand();
        drawTrail(graphics, partialTick, screenWidth, screenHeight);
        drawStrip(graphics, mainHand, mainStack, screenWidth * 0.5f + 91.0f + 13.0f, screenHeight - 13.0f, true, partialTick,
                stroking && strokeHand == InteractionHand.MAIN_HAND);
        drawStrip(graphics, offHand, offStack, screenWidth * 0.5f - 91.0f - 29.0f - 13.0f, screenHeight - 13.0f, false, partialTick,
                stroking && strokeHand == InteractionHand.OFF_HAND);
        drawPendingTimer(graphics, screenWidth, screenHeight, held);
        HudDraw.flushGlow();
        graphics.flush();
    }

    private static void drawTrail(GuiGraphics graphics, float partialTick, int width, int height) {
        int size = ClientState.TRAIL.size();
        if (size < 2) {
            return;
        }
        int start = 0;
        while (start < size) {
            int trailId = ClientState.TRAIL.get(start).trailId();
            int end = start + 1;
            while (end < size && ClientState.TRAIL.get(end).trailId() == trailId) {
                end++;
            }
            if (end - start >= 2) {
                drawTrailSegment(graphics, start, end, width, height, partialTick);
            }
            start = end;
        }
    }

    private static void drawTrailSegment(GuiGraphics graphics, int from, int to, int width, int height, float partialTick) {
        int capacity = to - from;
        float[] xs = new float[capacity];
        float[] ys = new float[capacity];
        float[] fades = new float[capacity];
        int count = 0;
        for (int i = from; i < to; i++) {
            ClientState.TrailPoint point = ClientState.TRAIL.get(i);
            float fade = trailFade(point);
            if (fade <= 0.0f) {
                continue;
            }
            xs[count] = point.x();
            ys[count] = point.y();
            fades[count] = fade;
            count++;
        }
        if (count < 2) {
            return;
        }

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = from; i < to; i++) {
            ClientState.TrailPoint point = ClientState.TRAIL.get(i);
            minX = Math.min(minX, point.x());
            maxX = Math.max(maxX, point.x());
            minY = Math.min(minY, point.y());
            maxY = Math.max(maxY, point.y());
        }
        float offsetX = width * 0.5f - (minX + maxX) * 0.5f;
        float offsetY = height * 0.5f - (minY + maxY) * 0.5f;
        for (int i = 0; i < count; i++) {
            xs[i] += offsetX;
            ys[i] += offsetY;
        }

        Archetype held = ClientState.TRAIL.get(from).archetype();
        boolean energy = held == Archetype.SHIELD || held == Archetype.BOW;
        int glowColor;
        int deepColor;
        int bodyBase;
        int bodyMix;
        int wetColor;
        int brightColor;
        if (held == Archetype.SHIELD) {
            glowColor = 0xFF0A5A2A;
            deepColor = 0xFF03240F;
            bodyBase = 0xFF0C5A2B;
            bodyMix = 0xFF1F9A55;
            wetColor = 0xFF1F9A55;
            brightColor = 0xFF7CFFB0;
        } else if (held == Archetype.BOW) {
            glowColor = 0xFF5A1E00;
            deepColor = 0xFF2A0E00;
            bodyBase = 0xFF6A2600;
            bodyMix = 0xFFB84A00;
            wetColor = 0xFFB84A00;
            brightColor = 0xFFFFC45A;
        } else {
            glowColor = WOUND_GLOW;
            deepColor = WOUND_DEEP;
            bodyBase = WOUND_BODY;
            bodyMix = WOUND_WET;
            wetColor = WOUND_WET;
            brightColor = WOUND_BRIGHT;
        }

        float[] widths = new float[count];
        float[] under = new float[count];
        float[] wet = new float[count];
        float[] sheen = new float[count];
        int[] underColors = new int[count];
        int[] bodyColors = new int[count];
        int[] wetColors = new int[count];
        int[] sheenColors = new int[count];

        for (int i = 0; i < count; i++) {
            float t = count <= 1 ? 1.0f : i / (float) (count - 1);
            float head = Mth.clamp(t, 0.0f, 1.0f);
            float dissolve = (float) (0.16 + 0.84 * Math.pow(head, 0.85));
            float swell = 1.0f + 0.18f * (float) Math.sin(Math.PI * Math.pow(head, 0.7));
            float base = (2.6f + 7.2f * fades[i]) * dissolve * swell * (energy ? 0.94f : 1.0f);
            widths[i] = Math.max(0.35f, base);
            under[i] = widths[i] * 1.40f;
            wet[i] = widths[i] * 0.44f;
            sheen[i] = Math.max(0.9f, widths[i] * 0.20f);
            underColors[i] = HudDraw.withAlpha(deepColor, 0.92f * fades[i]);
            int color = HudDraw.mix(bodyBase, bodyMix, 0.20f + 0.62f * head);
            bodyColors[i] = HudDraw.withAlpha(color, 0.97f * fades[i]);
            wetColors[i] = HudDraw.withAlpha(wetColor, (0.30f + 0.35f * head) * fades[i]);
            sheenColors[i] = HudDraw.withAlpha(brightColor, (0.10f + 0.50f * head) * fades[i]);
        }

        HudDraw.beginGlow(graphics, HudDraw.GLOW);
        for (int i = 1; i < count; i++) {
            float dx = xs[i] - xs[i - 1];
            float dy = ys[i] - ys[i - 1];
            if (dx * dx + dy * dy < 1.0f) {
                continue;
            }
            HudDraw.glowLine(graphics, xs[i - 1], ys[i - 1], xs[i], ys[i],
                    widths[i - 1] * 2.8f, widths[i] * 2.8f,
                    HudDraw.withAlpha(glowColor, 0.42f * fades[i - 1]),
                    HudDraw.withAlpha(glowColor, 0.42f * fades[i]));
        }
        HudDraw.endGlow();

        HudDraw.ribbon(graphics, xs, ys, under, underColors, count);
        HudDraw.ribbon(graphics, xs, ys, widths, bodyColors, count);
        HudDraw.ribbon(graphics, xs, ys, wet, wetColors, count);
        HudDraw.ribbon(graphics, xs, ys, sheen, sheenColors, count);

        int newest = count - 1;
        ClientState.TrailPoint lastPoint = ClientState.TRAIL.get(to - 1);
        float lastFade = trailFade(lastPoint);
        float age = ClientState.ticks - lastPoint.tick() + partialTick;
        if (age <= TRAIL_LIFE && lastFade > 0.0f) {
            float headX = xs[newest];
            float headY = ys[newest];
            HudDraw.disc(graphics, headX, headY, Math.max(1.0f, widths[newest] * 0.55f), HudDraw.withAlpha(wetColor, 0.9f * lastFade));
        }
    }

    private static float trailFade(ClientState.TrailPoint point) {
        float age = ClientState.ticks - point.tick();
        return Mth.clamp(1.0f - Math.max(0.0f, age) / TRAIL_LIFE, 0.0f, 1.0f);
    }

    private static int cooldownTicks(Technique technique, ItemStack stack, GestureShape shape) {
        WeaponSpecial special = WeaponProfiles.special(stack, shape);
        if (special != null) {
            return special.cooldownTicks();
        }
        return cooldownTicks(technique);
    }

    private static int cooldownTicks(Technique technique) {
        com.putzwirk.gesturalcombat.technique.AbilityConfig config =
                com.putzwirk.gesturalcombat.technique.AbilityConfigs.of(technique);
        return config != null && config.cooldownTicks() != null ? config.cooldownTicks() : technique.cooldownTicks();
    }

    private static String lastLoggedWeapon;

    private static void logResolution(ItemStack stack, Archetype held) {
        if (!com.putzwirk.gesturalcombat.Config.INSTANCE.debugCasts.get()) {
            return;
        }
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String key = id + "/" + held;
        if (key.equals(lastLoggedWeapon)) {
            return;
        }
        lastLoggedWeapon = key;
        com.putzwirk.gesturalcombat.Gesturalcombat.LOGGER.info(
                "gesturalcombat held: item={} archetype={} family={}", id, held,
                com.putzwirk.gesturalcombat.weapon.WeaponProfiles.family(stack));
    }

    private static void drawStrip(GuiGraphics graphics, Archetype held, ItemStack stack, float originX, float y,
                                  boolean rightward, float partialTick, boolean highlighted) {
        if (held == null) {
            return;
        }
        logResolution(stack, held);
        Minecraft mc = Minecraft.getInstance();
        GestureShape shown = highlighted && ClientState.stroking && ClientState.shownShape != null && ClientState.shownShape.isKnown()
                ? ClientState.shownShape : null;

        int slot = 0;
        java.util.Set<String> drawnIcons = new java.util.HashSet<>();
        for (GestureShape shape : STRIP) {
            Technique technique = Techniques.available(held, shape, stack);
            if (technique == null) {
                continue;
            }
            String id = technique.id();
            boolean detected = shown == shape;
            boolean casting = ClientState.flashTicks > 0 && id.equals(ClientState.lastTechnique);
            float cx = rightward ? originX + SLOT_SPACING * slot : originX - SLOT_SPACING * slot;
            slot++;

            boolean available = !ClientState.hidden(!rightward) && !ClientState.busy();
            String cooldownKey = CooldownKeys.of(stack, shape, technique);
            int remaining = ClientState.cooldownRemaining(cooldownKey);
            boolean cooling = remaining > 0;
            int preview = cooldownTicks(technique, stack, shape);
            if (com.putzwirk.gesturalcombat.weapon.DualWield.paired(mc.player)) {
                preview = (int) Math.round(preview * com.putzwirk.gesturalcombat.weapon.DualWield.COOLDOWN_SCALE);
            }
            float total = Math.max(1, ClientState.cooldownTotal(cooldownKey, preview));
            float fraction = cooling ? Mth.clamp((remaining - partialTick) / total, 0.0f, 1.0f) : 0.0f;

            float previous = LAST_FRACTION.getOrDefault(id, 0.0f);
            if (previous > 0.0f && !cooling) {
                READY_AT.put(id, ClientState.ticks);
            }
            LAST_FRACTION.put(id, fraction);

            float glow = 0.0f;
            Integer readyAt = READY_AT.get(id);
            if (readyAt != null) {
                int age = ClientState.ticks - readyAt;
                if (age <= READY_FLASH) {
                    glow = 1.0f - (age + partialTick) / (float) READY_FLASH;
                } else {
                    READY_AT.remove(id);
                }
            }

            float half = PLATE_SIZE * 0.5f;

            if (glow > 0.0f) {
                float spread = 2.0f + (1.0f - glow) * 4.0f;
                HudDraw.roundedRect(graphics, cx - half - spread, y - half - spread,
                        (half + spread) * 2.0f, (half + spread) * 2.0f, 7.0f + spread,
                        HudDraw.withAlpha(held.color(), 0.35f * glow));
                HudDraw.ring(graphics, cx, y, half + 1.0f, 1.4f, 0.0f, 360.0f,
                        HudDraw.withAlpha(held.color(), 0.85f * glow), 28);
            }

            int plate = PLATE;
            if (detected) {
                plate = HudDraw.mix(PLATE, HudDraw.withAlpha(held.color(), 0.55f), 0.5f);
            }
            float cast = casting ? Mth.clamp(ClientState.flashTicks / 14.0f, 0.0f, 1.0f) : 0.0f;
            if (cast > 0.0f) {
                plate = HudDraw.mix(plate, HudDraw.withAlpha(held.color(), 0.75f), 0.45f * cast);
            }
            HudDraw.roundedRect(graphics, cx - half, y - half, PLATE_SIZE, PLATE_SIZE, 6.0f, plate);

            String icon = AbilityIcons.iconFor(technique);
            boolean textured = icon != null && drawnIcons.add(icon);
            int tint;
            if (!available) {
                tint = textured ? LOCKED_ICON : LOCKED;
            } else if (detected) {
                tint = 0xFFFFFFFF;
            } else {
                tint = textured ? 0xFFFFFFFF : held.color();
            }

            float alpha = cooling ? COOLING_ALPHA : 1.0f;

            float shake = 0.0f;
            if (detected && ClientState.rejectTicks > 0) {
                tint = REJECT;
                alpha = 1.0f;
                shake = Mth.sin(ClientState.rejectTicks * 1.3f) * 2.0f;
            }

            int color = alpha >= 1.0f ? tint : HudDraw.withAlpha(tint, alpha);
            if (textured) {
                AbilityIcons.draw(graphics, technique, cx + shake, y, GLYPH_SIZE, color);
            } else {
                HudDraw.drawGestureIcon(graphics, shape, cx + shake, y, GLYPH_SIZE, color);
            }

            if (available && cooling) {
                float radius = half - 1.0f;
                HudDraw.pie(graphics, cx, y, radius, fraction, 0x59000000);
                HudDraw.cooldownRing(graphics, cx, y, radius, 1.0f, fraction, HudDraw.withAlpha(held.color(), 0.80f));
                drawCooldownText(graphics, mc, remaining, cx, y - half - 5.5f);
            }

            if (detected) {
                HudDraw.ring(graphics, cx, y, half + 0.8f, 1.0f, 0.0f, 360.0f, 0x8CFFFFFF, 28);
            }
        }
    }

    private static void drawCooldownText(GuiGraphics graphics, Minecraft mc, int remainingTicks, float cx, float labelY) {
        String text = remainingTicks >= 200
                ? String.valueOf((remainingTicks + 19) / 20)
                : String.format(Locale.ROOT, "%.1f", remainingTicks / 20.0f);
        float y = Mth.clamp(labelY, 2.0f, mc.getWindow().getGuiScaledHeight() - 6.0f);
        graphics.pose().pushPose();
        graphics.pose().translate(cx, y, 0.0f);
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        int width = mc.font.width(text);
        graphics.drawString(mc.font, text, -width / 2, 0, 0xFFF4F4F4, true);
        graphics.pose().popPose();
    }

    private static void drawPendingTimer(GuiGraphics graphics, int width, int height, Archetype held) {
        if (ClientState.pendingTicks <= 0 || ClientState.pendingTechnique.isEmpty()) {
            return;
        }
        float seconds = ClientState.pendingTicks / 20.0f;
        String text = String.format(Locale.ROOT, "%.1f", seconds);
        int x = (width - Minecraft.getInstance().font.width(text)) / 2;
        int y = height / 2 - 18;
        int color = held != null ? held.color() : 0xFFFFFFFF;
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color, true);
    }


}
