package com.putzwirk.gesturalcombat.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.putzwirk.gesturalcombat.core.GestureShape;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class HudDraw {
    public static final ResourceLocation GLOW = new ResourceLocation("gesturalcombat", "textures/gui/glow.png");
    public static final ResourceLocation SPARK = new ResourceLocation("gesturalcombat", "textures/gui/spark.png");

    private static final float[] POLY = new float[80];

    private static BufferBuilder glowBuffer;
    private static ResourceLocation glowTexture;
    private static boolean glowActive;
    private static int glowVertices;

    private HudDraw() {
    }

    public static float ease(float current, float target, float step) {
        return Mth.lerp(Mth.clamp(step, 0.0f, 1.0f), current, target);
    }

    public static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int withAlpha(int color, float multiplier) {
        int a = Math.round(alpha(color) * Mth.clamp(multiplier, 0.0f, 1.0f));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int scaleAlpha(int color, float multiplier) {
        return withAlpha(color, multiplier);
    }

    public static int mix(int a, int b, float t) {
        float f = Mth.clamp(t, 0.0f, 1.0f);
        int aa = Math.round(Mth.lerp(f, alpha(a), alpha(b)));
        int ar = Math.round(Mth.lerp(f, red(a), red(b)));
        int ag = Math.round(Mth.lerp(f, green(a), green(b)));
        int ab = Math.round(Mth.lerp(f, blue(a), blue(b)));
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    private static VertexConsumer gui(GuiGraphics graphics) {
        return graphics.bufferSource().getBuffer(RenderType.gui());
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, int color) {
        consumer.vertex(matrix, x, y, 0.0f)
                .color(red(color) / 255.0f, green(color) / 255.0f, blue(color) / 255.0f, alpha(color) / 255.0f)
                .endVertex();
    }

    public static void rect(GuiGraphics graphics, float x, float y, float width, float height, int color) {
        if (width <= 0.0f || height <= 0.0f || alpha(color) == 0) {
            return;
        }
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        vertex(consumer, matrix, x, y, color);
        vertex(consumer, matrix, x, y + height, color);
        vertex(consumer, matrix, x + width, y + height, color);
        vertex(consumer, matrix, x + width, y, color);
    }

    public static void tri(GuiGraphics graphics, float ax, float ay, float bx, float by, float cx, float cy, int color) {
        if (alpha(color) == 0) {
            return;
        }
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        vertex(consumer, matrix, ax, ay, color);
        vertex(consumer, matrix, bx, by, color);
        vertex(consumer, matrix, cx, cy, color);
        vertex(consumer, matrix, cx, cy, color);
    }

    public static void roundedRect(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0f || height <= 0.0f || alpha(color) == 0) {
            return;
        }
        float r = Math.min(radius, Math.min(width, height) * 0.5f);
        if (r <= 0.5f) {
            rect(graphics, x, y, width, height, color);
            return;
        }
        float cx = x + width * 0.5f;
        float cy = y + height * 0.5f;
        int corners = 4;
        int steps = 5;
        int count = 0;
        float[] centersX = {x + width - r, x + width - r, x + r, x + r};
        float[] centersY = {y + r, y + height - r, y + height - r, y + r};
        float[] startAngle = {270.0f, 0.0f, 90.0f, 180.0f};
        for (int c = 0; c < corners; c++) {
            for (int s = 0; s <= steps; s++) {
                float angle = (float) Math.toRadians(startAngle[c] + 90.0f * s / steps);
                POLY[count * 2] = centersX[c] + Mth.cos(angle) * r;
                POLY[count * 2 + 1] = centersY[c] + Mth.sin(angle) * r;
                count++;
            }
        }
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            vertex(consumer, matrix, cx, cy, color);
            vertex(consumer, matrix, POLY[i * 2], POLY[i * 2 + 1], color);
            vertex(consumer, matrix, POLY[next * 2], POLY[next * 2 + 1], color);
            vertex(consumer, matrix, cx, cy, color);
        }
    }

    public static void thickLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 0.01f || alpha(color) == 0) {
            return;
        }
        float half = Math.max(0.5f, width * 0.5f);
        float nx = -dy / length * half;
        float ny = dx / length * half;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        vertex(consumer, matrix, x1 + nx, y1 + ny, color);
        vertex(consumer, matrix, x2 + nx, y2 + ny, color);
        vertex(consumer, matrix, x2 - nx, y2 - ny, color);
        vertex(consumer, matrix, x1 - nx, y1 - ny, color);
    }

    public static void taperedLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float widthStart, float widthEnd, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 0.01f || alpha(color) == 0) {
            return;
        }
        float nx = -dy / length;
        float ny = dx / length;
        float hs = Math.max(0.5f, widthStart * 0.5f);
        float he = Math.max(0.5f, widthEnd * 0.5f);
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        vertex(consumer, matrix, x1 + nx * hs, y1 + ny * hs, color);
        vertex(consumer, matrix, x2 + nx * he, y2 + ny * he, color);
        vertex(consumer, matrix, x2 - nx * he, y2 - ny * he, color);
        vertex(consumer, matrix, x1 - nx * hs, y1 - ny * hs, color);
        if (hs >= 0.9f) {
            disc(graphics, x1, y1, hs, color);
        }
        if (he >= 0.9f) {
            disc(graphics, x2, y2, he, color);
        }
    }

    public static void ribbon(GuiGraphics graphics, float[] xs, float[] ys, float[] widths, int[] colors, int count) {
        if (count < 2 || xs == null || ys == null || widths == null || colors == null) {
            return;
        }
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        float prevLeftX = 0.0f;
        float prevLeftY = 0.0f;
        float prevRightX = 0.0f;
        float prevRightY = 0.0f;
        for (int i = 0; i < count; i++) {
            int from = Math.max(0, i - 1);
            int to = Math.min(count - 1, i + 1);
            float dirX = xs[to] - xs[from];
            float dirY = ys[to] - ys[from];
            float length = Mth.sqrt(dirX * dirX + dirY * dirY);
            if (length < 1.0e-4f) {
                dirX = 1.0f;
                dirY = 0.0f;
                length = 1.0f;
            }
            float nx = -dirY / length;
            float ny = dirX / length;
            float half = Math.max(0.35f, widths[i] * 0.5f);
            float leftX = xs[i] + nx * half;
            float leftY = ys[i] + ny * half;
            float rightX = xs[i] - nx * half;
            float rightY = ys[i] - ny * half;
            if (i > 0) {
                vertex(consumer, matrix, prevLeftX, prevLeftY, colors[i - 1]);
                vertex(consumer, matrix, leftX, leftY, colors[i]);
                vertex(consumer, matrix, rightX, rightY, colors[i]);
                vertex(consumer, matrix, prevRightX, prevRightY, colors[i - 1]);
            }
            prevLeftX = leftX;
            prevLeftY = leftY;
            prevRightX = rightX;
            prevRightY = rightY;
        }
    }

    public static void disc(GuiGraphics graphics, float cx, float cy, float radius, int color) {
        if (radius <= 0.05f || alpha(color) == 0) {
            return;
        }
        int segments = Math.max(6, Math.min(16, Math.round(radius * 5.0f)));
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        float prevX = cx + radius;
        float prevY = cy;
        for (int i = 1; i <= segments; i++) {
            float angle = (float) (Math.PI * 2.0 * i / segments);
            float x = cx + Mth.cos(angle) * radius;
            float y = cy + Mth.sin(angle) * radius;
            vertex(consumer, matrix, cx, cy, color);
            vertex(consumer, matrix, x, y, color);
            vertex(consumer, matrix, prevX, prevY, color);
            vertex(consumer, matrix, cx, cy, color);
            prevX = x;
            prevY = y;
        }
    }

    public static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color) {
        thickLine(graphics, x1, y1, x2, y2, 1.0f, color);
    }

    public static void ring(GuiGraphics graphics, float cx, float cy, float radius, float thickness, float startDegrees, float sweepDegrees, int color, int segments) {
        if (segments <= 0 || alpha(color) == 0) {
            return;
        }
        float inner = Math.max(0.5f, radius - thickness * 0.5f);
        float outer = radius + thickness * 0.5f;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = gui(graphics);
        for (int i = 0; i < segments; i++) {
            float a0 = (float) Math.toRadians(startDegrees + sweepDegrees * i / segments);
            float a1 = (float) Math.toRadians(startDegrees + sweepDegrees * (i + 1) / segments);
            float cos0 = Mth.cos(a0);
            float sin0 = Mth.sin(a0);
            float cos1 = Mth.cos(a1);
            float sin1 = Mth.sin(a1);
            vertex(consumer, matrix, cx + cos0 * inner, cy + sin0 * inner, color);
            vertex(consumer, matrix, cx + cos1 * inner, cy + sin1 * inner, color);
            vertex(consumer, matrix, cx + cos1 * outer, cy + sin1 * outer, color);
            vertex(consumer, matrix, cx + cos0 * outer, cy + sin0 * outer, color);
        }
    }

    public static void cooldownRing(GuiGraphics graphics, float cx, float cy, float radius, float thickness, float fraction, int color) {
        float f = Mth.clamp(fraction, 0.0f, 1.0f);
        if (f <= 0.0f) {
            return;
        }
        ring(graphics, cx, cy, radius, thickness, -90.0f, 360.0f * f, color, Math.max(1, Math.round(48 * f)));
    }

    public static void pie(GuiGraphics graphics, float cx, float cy, float radius, float fraction, int color) {
        float f = Mth.clamp(fraction, 0.0f, 1.0f);
        if (f <= 0.0f || alpha(color) == 0) {
            return;
        }
        int segments = Math.max(1, Math.round(40 * f));
        float start = -90.0f;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.toRadians(start + 360.0f * f * i / segments);
            double a1 = Math.toRadians(start + 360.0f * f * (i + 1) / segments);
            tri(graphics, cx, cy,
                    cx + Mth.cos((float) a0) * radius, cy + Mth.sin((float) a0) * radius,
                    cx + Mth.cos((float) a1) * radius, cy + Mth.sin((float) a1) * radius, color);
        }
    }

    public static void beginGlow(GuiGraphics graphics, ResourceLocation texture) {
        if (glowActive) {
            endGlow();
        }
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, texture);
        glowBuffer = Tesselator.getInstance().getBuilder();
        glowBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        glowTexture = texture;
        glowActive = true;
        glowVertices = 0;
    }

    public static void endGlow() {
        if (!glowActive) {
            return;
        }
        int vertices = glowVertices;
        BufferBuilder buffer = glowBuffer;
        glowBuffer = null;
        glowTexture = null;
        glowActive = false;
        glowVertices = 0;
        if (buffer == null) {
            return;
        }
        BufferBuilder.RenderedBuffer built = buffer.end();
        if (vertices > 0) {
            BufferUploader.drawWithShader(built);
        }
    }

    public static void flushGlow() {
        endGlow();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void texturedQuad(GuiGraphics graphics, ResourceLocation texture, float x1, float y1, float x2, float y2,
                                     float x3, float y3, float x4, float y4, int c1, int c2, int c3, int c4) {
        boolean batched = glowActive && texture.equals(glowTexture);
        if (!batched) {
            beginGlow(graphics, texture);
        }
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = glowBuffer;
        buffer.vertex(matrix, x1, y1, 0.0f).color(red(c1) / 255.0f, green(c1) / 255.0f, blue(c1) / 255.0f, alpha(c1) / 255.0f).uv(0.0f, 0.0f).endVertex();
        buffer.vertex(matrix, x2, y2, 0.0f).color(red(c2) / 255.0f, green(c2) / 255.0f, blue(c2) / 255.0f, alpha(c2) / 255.0f).uv(0.0f, 1.0f).endVertex();
        buffer.vertex(matrix, x3, y3, 0.0f).color(red(c3) / 255.0f, green(c3) / 255.0f, blue(c3) / 255.0f, alpha(c3) / 255.0f).uv(1.0f, 1.0f).endVertex();
        buffer.vertex(matrix, x4, y4, 0.0f).color(red(c4) / 255.0f, green(c4) / 255.0f, blue(c4) / 255.0f, alpha(c4) / 255.0f).uv(1.0f, 0.0f).endVertex();
        glowVertices += 4;
        if (!batched) {
            endGlow();
        }
    }

    public static void glowLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float widthStart, float widthEnd, int colorStart, int colorEnd) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 0.01f) {
            return;
        }
        if (alpha(colorStart) == 0 && alpha(colorEnd) == 0) {
            return;
        }
        float nx = -dy / length;
        float ny = dx / length;
        float hs = Math.max(0.5f, widthStart * 0.5f);
        float he = Math.max(0.5f, widthEnd * 0.5f);
        texturedQuad(graphics, GLOW,
                x1 + nx * hs, y1 + ny * hs,
                x2 + nx * he, y2 + ny * he,
                x2 - nx * he, y2 - ny * he,
                x1 - nx * hs, y1 - ny * hs,
                colorStart, colorEnd, colorEnd, colorStart);
    }

    public static void glowBlit(GuiGraphics graphics, ResourceLocation texture, float cx, float cy, float size, int color) {
        float half = size * 0.5f;
        texturedQuad(graphics, texture,
                cx - half, cy - half,
                cx - half, cy + half,
                cx + half, cy + half,
                cx + half, cy - half,
                color, color, color, color);
    }

    public static void text(GuiGraphics graphics, Font font, String value, float centerX, float centerY, int color, float scale, boolean shadow) {
        if (value == null || value.isEmpty() || alpha(color) == 0) {
            return;
        }
        float s = Math.max(0.25f, scale);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0f);
        graphics.pose().scale(s, s, 1.0f);
        int width = font.width(value);
        graphics.drawString(font, value, Math.round(-width * 0.5f), -4, color, shadow);
        graphics.pose().popPose();
    }

    public static void textLeft(GuiGraphics graphics, Font font, String value, float x, float y, int color, float scale, boolean shadow) {
        if (value == null || value.isEmpty() || alpha(color) == 0) {
            return;
        }
        float s = Math.max(0.25f, scale);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(s, s, 1.0f);
        graphics.drawString(font, value, 0, -4, color, shadow);
        graphics.pose().popPose();
    }

    public static void drawGestureIcon(GuiGraphics graphics, GestureShape shape, float cx, float cy, float size, int color) {
        if (shape == null || alpha(color) == 0) {
            return;
        }
        float half = Math.max(2.0f, size * 0.5f);
        float thickness = Math.max(1.0f, size * 0.075f);
        float head = Math.max(2.0f, size * 0.24f);
        switch (shape) {
            case LINE_H -> {
                float left = cx - half;
                float right = cx + half * 0.82f;
                thickLine(graphics, left, cy, right, cy, thickness, color);
                arrow(graphics, right, cy, 0.0f, head, thickness, color);
            }
            case LINE_V_UP -> {
                float bottom = cy + half * 0.8f;
                float top = cy - half * 0.8f;
                thickLine(graphics, cx, bottom, cx, top + head * 0.55f, thickness, color);
                arrow(graphics, cx, top, -Mth.HALF_PI, head, thickness, color);
            }
            case LINE_V_DOWN -> {
                float top = cy - half * 0.8f;
                float bottom = cy + half * 0.8f;
                thickLine(graphics, cx, top, cx, bottom - head * 0.55f, thickness, color);
                arrow(graphics, cx, bottom, Mth.HALF_PI, head, thickness, color);
            }
            case CIRCLE -> {
                float radius = half * 0.78f;
                ring(graphics, cx, cy, radius, thickness, -70.0f, 285.0f, color, 22);
                float endAngle = (float) Math.toRadians(-70.0f + 285.0f);
                float ex = cx + Mth.cos(endAngle) * radius;
                float ey = cy + Mth.sin(endAngle) * radius;
                arrow(graphics, ex, ey, endAngle + Mth.HALF_PI, head, thickness, color);
            }
            case ZIGZAG -> {
                float left = cx - half * 0.62f;
                float right = cx + half * 0.62f;
                float high = cy - half * 0.72f;
                float low = cy + half * 0.72f;
                thickLine(graphics, left, high, right, high, thickness, color);
                thickLine(graphics, right, high, left, low, thickness, color);
                thickLine(graphics, left, low, right, low, thickness, color);
            }
            case V -> {
                float left = cx - half * 0.82f;
                float right = cx + half * 0.82f;
                float top = cy - half * 0.72f;
                float bottom = cy + half * 0.72f;
                thickLine(graphics, left, top, cx, bottom, thickness, color);
                thickLine(graphics, cx, bottom, right, top, thickness, color);
            }
            case CARET -> {
                float left = cx - half * 0.82f;
                float right = cx + half * 0.82f;
                float top = cy - half * 0.72f;
                float bottom = cy + half * 0.72f;
                thickLine(graphics, left, bottom, cx, top, thickness, color);
                thickLine(graphics, cx, top, right, bottom, thickness, color);
            }
            case UNKNOWN -> {
                float radius = Math.max(1.4f, half * 0.28f);
                ring(graphics, cx, cy, radius, Math.max(1.0f, radius * 0.9f), 0.0f, 360.0f, color, 12);
            }
        }
    }

    public static void arrow(GuiGraphics graphics, float tipX, float tipY, float angle, float length, float thickness, int color) {
        float spread = 0.62f;
        float ax = tipX - (float) Math.cos(angle - spread) * length;
        float ay = tipY - (float) Math.sin(angle - spread) * length;
        float bx = tipX - (float) Math.cos(angle + spread) * length;
        float by = tipY - (float) Math.sin(angle + spread) * length;
        thickLine(graphics, tipX, tipY, ax, ay, thickness, color);
        thickLine(graphics, tipX, tipY, bx, by, thickness, color);
    }

    public static void curve(GuiGraphics graphics, float x0, float y0, float cxp, float cyp, float x1, float y1, float thickness, int color, int segments) {
        float px = x0;
        float py = y0;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float inv = 1.0f - t;
            float x = inv * inv * x0 + 2.0f * inv * t * cxp + t * t * x1;
            float y = inv * inv * y0 + 2.0f * inv * t * cyp + t * t * y1;
            thickLine(graphics, px, py, x, y, thickness, color);
            px = x;
            py = y;
        }
    }
}
