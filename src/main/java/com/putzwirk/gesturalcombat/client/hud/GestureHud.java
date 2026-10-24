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
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.Locale;

public final class GestureHud {
    private static final int TRAIL_LIFE = 18;
    private static final GestureShape[] STRIP = {
            GestureShape.LINE_H,
            GestureShape.LINE_V_UP,
            GestureShape.LINE_V_DOWN,
            GestureShape.CIRCLE,
            GestureShape.ZIGZAG,
            GestureShape.V,
            GestureShape.CARET
    };
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

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
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
        int[] seeds = new int[capacity];
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
            seeds[count] = trailSeed(point);
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
            float t = i / (float) (count - 1);
            float taper = (float) Math.pow(Math.sin(Math.PI * t), 0.55);
            int seed = seeds[i];
            float jag = energy ? 0.92f + 0.08f * trailNoise(seed) : 0.70f + 0.60f * trailNoise(seed);
            float base = (energy ? 2.2f + 5.4f * fades[i] : 2.0f + 6.2f * fades[i]) * taper * jag;
            widths[i] = Math.max(0.45f, base);
            under[i] = widths[i] * (energy ? 1.55f : 1.38f);
            wet[i] = widths[i] * (energy ? 0.34f : 0.42f);
            sheen[i] = Math.max(0.8f, widths[i] * (energy ? 0.22f : 0.17f));
            underColors[i] = HudDraw.withAlpha(deepColor, 0.96f * fades[i]);
            int color = HudDraw.mix(bodyBase, bodyMix, 0.12f + 0.55f * trailNoise(seed ^ 0x41));
            bodyColors[i] = HudDraw.withAlpha(color, 0.97f * fades[i]);
            wetColors[i] = HudDraw.withAlpha(wetColor, 0.55f * fades[i]);
            float sheenAlpha = trailNoise(seed ^ 0x8D) > 0.42f ? 0.55f : 0.10f;
            sheenColors[i] = HudDraw.withAlpha(brightColor, sheenAlpha * fades[i]);
        }

        HudDraw.beginGlow(graphics, HudDraw.GLOW);
        for (int i = 1; i < count; i++) {
            float dx = xs[i] - xs[i - 1];
            float dy = ys[i] - ys[i - 1];
            if (dx * dx + dy * dy < 1.0f) {
                continue;
            }
            HudDraw.glowLine(graphics, xs[i - 1], ys[i - 1], xs[i], ys[i],
                    widths[i - 1] * 2.3f, widths[i] * 2.3f,
                    HudDraw.withAlpha(glowColor, 0.35f * fades[i - 1]),
                    HudDraw.withAlpha(glowColor, 0.35f * fades[i]));
        }
        HudDraw.endGlow();

        HudDraw.ribbon(graphics, xs, ys, under, underColors, count);
        HudDraw.ribbon(graphics, xs, ys, widths, bodyColors, count);
        HudDraw.ribbon(graphics, xs, ys, wet, wetColors, count);
        HudDraw.ribbon(graphics, xs, ys, sheen, sheenColors, count);

        for (int i = 1; i < count - 1 && !energy; i++) {
            float fade = fades[i];
            int seed = seeds[i];
            float dx = xs[i + 1] - xs[i - 1];
            float dy = ys[i + 1] - ys[i - 1];
            float length = Mth.sqrt(dx * dx + dy * dy);
            if (length < 0.8f) {
                continue;
            }
            float nx = -dy / length;
            float ny = dx / length;
            float edge = widths[i] * 0.58f;
            if (trailNoise(seed ^ 0xE1) > 0.72f) {
                float spur = 2.5f + trailNoise(seed ^ 0xE5) * 6.0f;
                float side = trailNoise(seed ^ 0xE9) > 0.5f ? 1.0f : -1.0f;
                float baseX = xs[i] + nx * side * edge;
                float baseY = ys[i] + ny * side * edge;
                HudDraw.taperedLine(graphics, baseX, baseY,
                        baseX + nx * side * spur + dx * 0.15f,
                        baseY + ny * side * spur + dy * 0.15f,
                        Math.max(1.2f, widths[i] * 0.30f), 0.5f,
                        HudDraw.withAlpha(WOUND_WET, 0.75f * fade));
            }
            if (trailNoise(seed ^ 0xD1) > 0.80f && fade > 0.25f) {
                float dripLength = 8.0f + trailNoise(seed ^ 0xD7) * 18.0f;
                float dripWidth = Math.max(1.4f, widths[i] * 0.34f);
                drip(graphics, xs[i] + nx * edge * 0.5f, ys[i] + ny * edge * 0.5f, dripLength, dripWidth, fade);
            }
        }

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

    private static void drip(GuiGraphics graphics, float x, float y, float length, float width, float fade) {
        HudDraw.taperedLine(graphics, x, y, x, y + length, width, width * 0.55f,
                HudDraw.withAlpha(WOUND_WET, 0.92f * fade));
        HudDraw.disc(graphics, x, y + length, width * 0.72f, HudDraw.withAlpha(WOUND_WET, 0.92f * fade));
        HudDraw.taperedLine(graphics, x - width * 0.22f, y + 1.0f, x - width * 0.22f, y + length * 0.82f,
                Math.max(0.6f, width * 0.28f), 0.6f, HudDraw.withAlpha(WOUND_BRIGHT, 0.30f * fade));
        HudDraw.taperedLine(graphics, x + width * 0.18f, y, x + width * 0.18f, y + length * 0.7f,
                Math.max(0.5f, width * 0.18f), 0.5f, HudDraw.withAlpha(WOUND_DEEP, 0.55f * fade));
    }

    private static float trailFade(ClientState.TrailPoint point) {
        float age = ClientState.ticks - point.tick();
        return Mth.clamp(1.0f - Math.max(0.0f, age) / TRAIL_LIFE, 0.0f, 1.0f);
    }

    private static int trailSeed(ClientState.TrailPoint point) {
        int hash = point.tick() * 73856093;
        hash ^= Float.floatToIntBits(point.x()) * 19349663;
        hash ^= Float.floatToIntBits(point.y()) * 83492791;
        return hash;
    }

    private static float trailNoise(int seed) {
        int hash = seed;
        hash ^= hash >>> 15;
        hash *= 0x2C1B3C6D;
        hash ^= hash >>> 12;
        hash *= 0x297A2D39;
        hash ^= hash >>> 15;
        return (hash & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
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

    private static void drawStrip(GuiGraphics graphics, Archetype held, ItemStack stack, float originX, float y,
                                  boolean rightward, float partialTick, boolean highlighted) {
        if (held == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GestureShape shown = highlighted && ClientState.stroking && ClientState.shownShape != null && ClientState.shownShape.isKnown()
                ? ClientState.shownShape : null;

        int slot = 0;
        for (GestureShape shape : STRIP) {
            if (com.putzwirk.gesturalcombat.weapon.WeaponRules.blocked(stack, shape)) {
                continue;
            }
            Technique technique = Techniques.forGesture(held, shape, stack);
            if (technique == null) {
                continue;
            }
            String id = technique.id();
            boolean detected = shown == shape;
            boolean casting = ClientState.flashTicks > 0 && id.equals(ClientState.lastTechnique);
            float cx = rightward ? originX + SLOT_SPACING * slot : originX - SLOT_SPACING * slot;
            slot++;

            boolean available = ClientState.MASTERY.isTechniqueAvailable(technique)
                    && !ClientState.hidden(!rightward)
                    && !ClientState.busy();
            String cooldownKey = CooldownKeys.of(stack, shape, technique);
            int remaining = ClientState.cooldownRemaining(cooldownKey);
            boolean cooling = remaining > 0;
            float total = Math.max(1, ClientState.cooldownTotal(cooldownKey, cooldownTicks(technique, stack, shape)));
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

            boolean textured = AbilityIcons.present(id);
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
                AbilityIcons.draw(graphics, id, cx + shake, y, GLYPH_SIZE, color);
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
