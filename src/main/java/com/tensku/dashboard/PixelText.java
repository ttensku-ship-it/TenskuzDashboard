package com.tensku.dashboard;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Draws text in ANY font installed on your PC. The text is rasterized with Java2D once,
 * cached as small horizontal runs, and drawn with fill(). No texture uploads needed.
 */
public final class PixelText {
    private PixelText() {}

    record Run(int x, int y, int len, int a) {}
    record Cached(List<Run> runs, int w, int h) {}

    static final Map<String, Cached> CACHE = new HashMap<>();
    static List<String> fontNames;
    public static final String[] EFFECTS = {"None", "Shadow", "Rainbow", "Wave", "Pulse", "Glow"};

    public static List<String> fonts() {
        if (fontNames == null) {
            Set<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            try {
                s.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            } catch (Throwable ignored) { }
            List<String> l = new ArrayList<>(List.of("SansSerif", "Serif", "Monospaced", "Dialog"));
            for (String n : s) if (!l.contains(n)) l.add(n);
            fontNames = l;
        }
        return fontNames;
    }

    static int lvl(int a) { return a >= 192 ? 255 : a >= 128 ? 170 : a >= 64 ? 100 : 0; }

    static Cached get(String font, int size, String text) {
        size = Math.max(8, Math.min(96, size));
        if (text.length() > 60) text = text.substring(0, 60);
        String key = font + "|" + size + "|" + text;
        Cached c = CACHE.get(key);
        if (c != null) return c;
        if (CACHE.size() > 80) CACHE.clear();
        List<Run> runs = new ArrayList<>();
        int w = 1, h = 1;
        try {
            Font f = new Font(font, Font.BOLD, size);
            BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = tmp.createGraphics();
            FontMetrics fm = tg.getFontMetrics(f);
            w = Math.max(1, fm.stringWidth(text)) + 6;
            h = fm.getHeight() + 4;
            int asc = fm.getAscent();
            tg.dispose();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gg.setFont(f);
            gg.setColor(Color.WHITE);
            gg.drawString(text, 3, asc + 2);
            gg.dispose();
            int[] px = new int[w * h];
            img.getRGB(0, 0, w, h, px, 0, w);
            for (int y = 0; y < h; y++) {
                int x = 0;
                while (x < w) {
                    int al = lvl(px[y * w + x] >>> 24);
                    if (al == 0) { x++; continue; }
                    int s = x;
                    while (x < w && lvl(px[y * w + x] >>> 24) == al) x++;
                    runs.add(new Run(s, y, x - s, al));
                }
            }
        } catch (Throwable ignored) { }
        c = new Cached(runs, w, h);
        CACHE.put(key, c);
        return c;
    }

    static int argb(float a, int rgb) { return (Math.max(0, Math.min(255, (int) a)) << 24) | (rgb & 0xFFFFFF); }

    public static void draw(GuiGraphicsExtractor g, String text, float cx, float cy, int size, int rgb,
                            int effect, float time, float alpha) {
        if (text == null || text.isEmpty() || alpha <= 0.01f) return;
        Cached c = get(Settings.d.font, size, text);
        if (c.runs.isEmpty()) return;
        int x0 = Math.round(cx - c.w / 2f), y0 = Math.round(cy - c.h / 2f);

        if (effect == 1 || effect == 5)
            for (Run r : c.runs) g.fill(x0 + r.x + 1, y0 + r.y + 1, x0 + r.x + 1 + r.len, y0 + r.y + 2, argb(r.a * alpha * 0.7f, 0x000000));
        if (effect == 5) {
            int[][] o = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] d : o)
                for (Run r : c.runs) g.fill(x0 + r.x + d[0], y0 + r.y + d[1], x0 + r.x + d[0] + r.len, y0 + r.y + d[1] + 1, argb(r.a * alpha * 0.3f, rgb));
        }
        float pulse = effect == 4 ? 0.65f + 0.35f * (float) Math.sin(time * 4) : 1f;
        for (Run r : c.runs) {
            int col = rgb, yo = 0;
            if (effect == 2) col = Color.HSBtoRGB((r.x * 0.012f + time * 0.4f) % 1f, 0.75f, 1f);
            if (effect == 3) yo = Math.round((float) Math.sin(r.x * 0.08 + time * 3) * size * 0.12f);
            g.fill(x0 + r.x, y0 + r.y + yo, x0 + r.x + r.len, y0 + r.y + yo + 1, argb(r.a * alpha * pulse, col));
        }
    }

    /** The on-screen text feature. */
    public static void drawOverlay(GuiGraphicsExtractor g, int W, int H, float time) {
        Settings.Data s = Settings.d;
        if (!s.textOn) return;
        draw(g, s.text, W * s.tx / 100f, H * s.ty / 100f, s.textSize, s.textColor, s.effect, time, 1f);
    }
}
