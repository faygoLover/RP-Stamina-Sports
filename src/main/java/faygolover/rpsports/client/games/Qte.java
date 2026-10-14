package faygolover.rpsports.client.games;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * Рисование QTE в духе A Way Out: золотые значки — клавиша в кружке, кольцо вокруг (заполнение, время), дуга с
 * точкой и зоной (тайминг), стрелки, мелкий текст. Кольца и дуги — гладкие (треугольники), не по пикселям.
 */
public final class Qte {
    private Qte() {}

    public static final int GOLD = 0xFFE8C34A, GOLD_DIM = 0x70E8C34A, GOLD_ZONE = 0xFFFFE38A, WHITE = 0xFFFFFFFF,
            WHITE_DIM = 0x90FFFFFF, SHADE = 0x80000000, BAD = 0xFFE0604A, GOOD = 0xFF7FD07A;
    private static final float TAU = (float) (Math.PI * 2);

    /** Дуга-кольцо от {@code from} до {@code to} (доли круга, 0 — сверху, по часовой), толщина {@code w}. */
    public static void arc(GuiGraphics g, float cx, float cy, float r, float w, float from, float to, int argb) {
        if (to <= from) return;
        g.flush();
        Matrix4f m = g.pose().last().pose();
        int segs = Math.max(6, (int) ((to - from) * 96));
        float a = (argb >>> 24) / 255f, rr = (argb >> 16 & 255) / 255f, gg = (argb >> 8 & 255) / 255f, bb = (argb & 255) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        float ro = r + w / 2, ri = Math.max(0, r - w / 2);
        for (int i = 0; i <= segs; i++) {
            float t = from + (to - from) * i / segs;
            float ang = t * TAU - (float) Math.PI / 2;
            float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
            buf.vertex(m, cx + c * ro, cy + s * ro, 0).color(rr, gg, bb, a).endVertex();
            buf.vertex(m, cx + c * ri, cy + s * ri, 0).color(rr, gg, bb, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
    }

    /** Круг (диск). */
    public static void disk(GuiGraphics g, float cx, float cy, float r, int argb) {
        arc(g, cx, cy, r / 2, r, 0, 1, argb);
    }

    /** Точка на дуге. */
    public static void dot(GuiGraphics g, float cx, float cy, float r, float frac, float size, int argb) {
        float ang = frac * TAU - (float) Math.PI / 2;
        disk(g, cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r, size, argb);
    }

    /**
     * Клавиша в кружке: тёмный круг, золотое кольцо, подпись. {@code press} 0…1 — нажатие: кружок чуть меньше и
     * светлее (клавиша «нажимается»).
     */
    public static void key(GuiGraphics g, Font font, float cx, float cy, float r, String label, float press, boolean active) {
        float k = 1 - 0.12f * press;
        disk(g, cx, cy, r * k, press > 0 ? 0xA0302610 : SHADE);
        arc(g, cx, cy, r * k - 0.6f, 1.2f, 0, 1, active ? GOLD : GOLD_DIM);
        float scale = label.length() > 1 ? 0.6f : 1f;
        g.pose().pushPose();
        g.pose().translate(cx, cy + (press > 0 ? 0.5f : 0), 0);
        g.pose().scale(scale * k, scale * k, 1);
        int tw = font.width(label);
        g.drawString(font, label, -tw / 2f, -4f, active ? GOLD : GOLD_DIM, false);
        g.pose().popPose();
    }

    /** Широкая клавиша-«пилюля» (пробел): заполнение {@code fill} слева направо. */
    public static void wideKey(GuiGraphics g, Font font, float cx, float cy, float w, float h, String label, float fill, float press) {
        float k = 1 - 0.08f * press;
        float x0 = cx - w * k / 2, x1 = cx + w * k / 2, y0 = cy - h * k / 2, y1 = cy + h * k / 2;
        g.pose().pushPose();
        g.pose().translate(0, press > 0 ? 0.5f : 0, 0);
        g.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), SHADE);
        if (fill > 0) g.fill(Math.round(x0) + 1, Math.round(y0) + 1, Math.round(x0 + 1 + (x1 - x0 - 2) * Math.min(1, fill)), Math.round(y1) - 1, 0x80E8C34A);
        g.renderOutline(Math.round(x0), Math.round(y0), Math.round(x1 - x0), Math.round(y1 - y0), GOLD);
        small(g, font, Component.literal(label), cx, cy - 2.5f, GOLD);
        g.pose().popPose();
    }

    /** Стрелка: 0 — вверх, 1 — вправо, 2 — вниз, 3 — влево. */
    public static void arrow(GuiGraphics g, float cx, float cy, float size, int dir, int argb) {
        g.flush();
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90 * dir));
        Matrix4f m = g.pose().last().pose();
        float a = (argb >>> 24) / 255f, rr = (argb >> 16 & 255) / 255f, gg = (argb >> 8 & 255) / 255f, bb = (argb & 255) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float s = size / 2;
        // Наконечник.
        buf.vertex(m, 0, -s, 0).color(rr, gg, bb, a).endVertex();
        buf.vertex(m, -s * 0.9f, 0, 0).color(rr, gg, bb, a).endVertex();
        buf.vertex(m, s * 0.9f, 0, 0).color(rr, gg, bb, a).endVertex();
        // Стержень.
        float w = s * 0.32f;
        buf.vertex(m, -w, 0, 0).color(rr, gg, bb, a).endVertex();
        buf.vertex(m, -w, s, 0).color(rr, gg, bb, a).endVertex();
        buf.vertex(m, w, s, 0).color(rr, gg, bb, a).endVertex();
        buf.vertex(m, -w, 0, 0).color(rr, gg, bb, a).endVertex();
        buf.vertex(m, w, s, 0).color(rr, gg, bb, a).endVertex();
        buf.vertex(m, w, 0, 0).color(rr, gg, bb, a).endVertex();
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
        g.pose().popPose();
    }

    /** Мелкий текст по центру (0,7 размера). */
    public static void small(GuiGraphics g, Font font, Component text, float cx, float y, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(0.7f, 0.7f, 1);
        g.drawString(font, text.getVisualOrderText(), -font.width(text) / 2f, 0, color, true);
        g.pose().popPose();
    }

    /** Тонкая полоса со смещением 0…1 и зоной. */
    public static void track(GuiGraphics g, int x, int y, int w, float zoneFrom, float zoneTo, float marker, int markerColor) {
        g.fill(x, y, x + w, y + 3, SHADE);
        g.fill(x, y + 1, x + w, y + 2, GOLD_DIM);
        int a = x + Math.round(Math.max(0, zoneFrom) * w), b = x + Math.round(Math.min(1, zoneTo) * w);
        if (b > a) g.fill(a, y, b, y + 3, GOLD_ZONE);
        float mx = x + Math.max(0, Math.min(1, marker)) * w;
        disk(g, mx, y + 1.5f, 4.5f, markerColor);
    }
}
