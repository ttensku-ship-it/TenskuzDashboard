package com.tensku.dashboard;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Animated backgrounds. Every theme draws inside the rectangle you give it, so it works for previews too. */
public final class Themes {
    private Themes() {}

    public static final String[] NAMES = {"Midnight Stars", "City Rain", "Synthwave", "Aurora", "Matrix", "Snow Night"};

    // ---------- helpers ----------
    static float r(int a, int b) {
        int x = a * 374761393 + b * 668265263;
        x = (x ^ (x >>> 13)) * 1274126177;
        x ^= x >>> 16;
        return (x & 0xFFFF) / 65535f;
    }

    static int al(int rgb, float a) { return (Math.max(0, Math.min(255, (int) (a * 255))) << 24) | (rgb & 0xFFFFFF); }

    static int lerp(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int rr = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int gg = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int bb = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (rr << 16) | (gg << 8) | bb;
    }

    static void vgrad(GuiGraphicsExtractor g, int x, int y, int w, int h, int c1, int c2) {
        int n = Math.max(2, Math.min(h, 28));
        for (int i = 0; i < n; i++)
            g.fill(x, y + h * i / n, x + w, y + h * (i + 1) / n, lerp(c1, c2, i / (float) (n - 1)));
    }

    /** fill clamped to a box */
    static void fc(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int col, int bx, int by, int bw, int bh) {
        x1 = Math.max(x1, bx); y1 = Math.max(y1, by);
        x2 = Math.min(x2, bx + bw); y2 = Math.min(y2, by + bh);
        if (x2 > x1 && y2 > y1) g.fill(x1, y1, x2, y2, col);
    }

    static float mod(float a, float b) { float m = a % b; return m < 0 ? m + b : m; }

    // ---------- entry ----------
    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, int theme, float t) {
        switch (Math.floorMod(theme, NAMES.length)) {
            case 0 -> midnight(g, x, y, w, h, t);
            case 1 -> cityRain(g, x, y, w, h, t);
            case 2 -> synthwave(g, x, y, w, h, t);
            case 3 -> aurora(g, x, y, w, h, t);
            case 4 -> matrix(g, x, y, w, h, t);
            default -> snow(g, x, y, w, h, t);
        }
    }

    // ---------- 0: Midnight Stars ----------
    static void stars(GuiGraphicsExtractor g, int x, int y, int w, int h, float t, int n) {
        for (int i = 0; i < n; i++) {
            int sx = x + (int) (r(i, 1) * w), sy = y + (int) (r(i, 2) * h);
            float tw = 0.4f + 0.6f * (float) Math.sin(t * (1 + r(i, 3) * 2) + i);
            int s = r(i, 4) > 0.9f ? 2 : 1;
            g.fill(sx, sy, sx + s, sy + s, al(0xFFFFFF, Math.max(0.1f, tw)));
        }
    }

    static void midnight(GuiGraphicsExtractor g, int x, int y, int w, int h, float t) {
        vgrad(g, x, y, w, h, 0xFF050816, 0xFF1A1F4A);
        stars(g, x, y, w, h, t, Math.max(20, w * h / 600));
        int cx = x + (int) (w * 0.8f), cy = y + (int) (h * 0.25f), rad = Math.max(4, (int) (h * 0.1f));
        for (int dy = -rad; dy <= rad; dy++) {
            int half = (int) Math.sqrt(rad * rad - dy * dy);
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xFFE8ECFF);
        }
        for (int dy = -rad - 4; dy <= rad + 4; dy += 2) {
            int half = (int) Math.sqrt(Math.max(0, (rad + 4) * (rad + 4) - dy * dy));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 2, 0x10AAB8FF);
        }
    }

    // ---------- 1: City Rain (cinematic) ----------
    static void skyline(GuiGraphicsExtractor g, int x, int y, int w, int h, int base, float t,
                        int seed, float speed, int minH, int maxH, int color, boolean windows) {
        int n = 16;
        int[] bw = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) { bw[i] = 16 + (int) (r(seed, i) * 26); total += bw[i] + 2; }
        float off = -mod(t * speed, total);
        float cur = off;
        int guard = 0;
        while (cur < w && guard++ < 80) {
            for (int i = 0; i < n && cur < w; i++) {
                int bx = x + (int) cur, bwid = bw[i];
                int bh = minH + (int) (r(seed + 7, i) * (maxH - minH));
                int top = base - bh;
                fc(g, bx, top, bx + bwid, base, color, x, y, w, h);
                if (windows) {
                    for (int wy = top + 4; wy < base - 4; wy += 7)
                        for (int wx = bx + 3; wx < bx + bwid - 3; wx += 5) {
                            float v = r(seed * 31 + i, (wx - bx) * 13 + (wy - top));
                            boolean lit = v > 0.55f;
                            if (r(seed + i, ((wx - bx) * 7 + wy) + (int) (t * 0.6f)) > 0.985f) lit = !lit;
                            if (lit) {
                                int c = v > 0.85f ? 0xFF5CE1FF : 0xFFFFD27A;
                                fc(g, wx, wy, wx + 2, wy + 3, c, x, y, w, h);
                            }
                        }
                    if (i % 4 == 0) { // neon sign
                        float p = 0.6f + 0.4f * (float) Math.sin(t * 3 + i);
                        int nc = i % 8 == 0 ? 0xFF2E9A : 0x2EE6FF;
                        fc(g, bx + 2, top - 3, bx + bwid - 2, top - 1, al(nc, p), x, y, w, h);
                        fc(g, bx, top - 6, bx + bwid, top, al(nc, 0.12f * p), x, y, w, h);
                    }
                }
                cur += bwid + 2;
            }
        }
    }

    static void cityRain(GuiGraphicsExtractor g, int x, int y, int w, int h, float t) {
        vgrad(g, x, y, w, h, 0xFF0A0E1E, 0xFF2C1B4D);
        int base = y + h - (int) (h * 0.14f);
        skyline(g, x, y, w, h, base, t, 3, 2.5f, (int) (h * 0.25f), (int) (h * 0.55f), 0xFF121631, false);
        skyline(g, x, y, w, h, base, t, 9, 7f, (int) (h * 0.2f), (int) (h * 0.6f), 0xFF0A0C1C, true);
        // wet road + reflections
        g.fill(x, base, x + w, y + h, 0xFF06070D);
        for (int i = 0; i < 7; i++) {
            int rx = x + (int) (r(i, 40) * w);
            float p = 0.15f + 0.1f * (float) Math.sin(t * 2 + i);
            int c = i % 2 == 0 ? 0xFF2E9A : 0x2EE6FF;
            g.fill(rx, base, rx + 3, y + h, al(c, p));
        }
        // cars (head + tail lights)
        for (int i = 0; i < 3; i++) {
            int cx = x + (int) mod(t * (40 + i * 25) + i * 97, w);
            int cy = base + 4 + i * 4;
            if (cy + 2 < y + h) {
                g.fill(cx, cy, cx + 2, cy + 2, 0xFFFFF2C0);
                g.fill(cx - 7, cy, cx - 5, cy + 2, 0xFFFF3040);
                g.fill(cx - 12, cy, cx + 6, cy + 2, 0x18FFFFFF);
            }
        }
        // rain
        int drops = Math.max(40, w / 4);
        for (int i = 0; i < drops; i++) {
            float sp = 230 + r(i, 11) * 140;
            float yy = mod(r(i, 12) * h + t * sp, h + 12) - 10;
            float xx = mod(r(i, 13) * (w + 60) - yy * 0.25f, w + 60) - 30;
            int dx = x + (int) xx, dy = y + (int) yy;
            if (dy >= y && dy + 8 <= y + h && dx > x + 1 && dx < x + w - 1) {
                g.fill(dx, dy, dx + 1, dy + 4, 0x66AFC8FF);
                g.fill(dx - 1, dy + 4, dx, dy + 8, 0x66AFC8FF);
            }
        }
        // ground splashes
        for (int i = 0; i < 14; i++) {
            float ph = mod(t * 3 + r(i, 50) * 5, 1f);
            if (ph < 0.4f) {
                int sx = x + (int) (r(i, 51) * w), sy = base + 2 + (int) (r(i, 52) * (y + h - base - 4));
                g.fill(sx - 2, sy, sx + 3, sy + 1, al(0xAFC8FF, 0.5f * (1 - ph / 0.4f)));
            }
        }
        // lightning
        int bucket = (int) Math.floor(t / 7f);
        float at = bucket * 7f + r(bucket, 9) * 6f, dt = t - at;
        if (r(bucket, 7) > 0.3f && dt >= 0 && dt < 0.5f) {
            float f = dt < 0.12f ? 0.5f : dt < 0.2f ? 0.05f : dt < 0.3f ? 0.35f * (1 - (dt - 0.2f) / 0.1f) : 0f;
            if (f > 0) g.fill(x, y, x + w, y + h, al(0xCFE0FF, f));
        }
        // vignette + cinematic bars
        g.fill(x, y, x + 3, y + h, 0x30000000);
        g.fill(x + w - 3, y, x + w, y + h, 0x30000000);
        int bar = Math.max(2, (int) (h * 0.07f));
        g.fill(x, y, x + w, y + bar, 0xFF000000);
        g.fill(x, y + h - bar, x + w, y + h, 0xFF000000);
    }

    // ---------- 2: Synthwave ----------
    static void synthwave(GuiGraphicsExtractor g, int x, int y, int w, int h, float t) {
        int hor = y + (int) (h * 0.58f);
        vgrad(g, x, y, w, hor - y, 0xFF14002E, 0xFFB3288A);
        g.fill(x, hor, x + w, y + h, 0xFF12001F);
        stars(g, x, y, w, (hor - y) / 2, t, Math.max(10, w / 6));
        int cx = x + w / 2, rad = (int) (h * 0.24f), cy = hor - (int) (rad * 0.35f);
        for (int dy = -rad; dy <= 0 + rad; dy++) {
            int py = cy + dy;
            if (py >= hor) break;
            if (dy > 0 && ((dy + rad) % 8) > (6 - dy * 6 / rad)) continue; // stripes
            int half = (int) Math.sqrt(rad * rad - dy * dy);
            g.fill(cx - half, py, cx + half, py + 1, lerp(0xFFFFE05A, 0xFFFF2E9A, (dy + rad) / (2f * rad)));
        }
        int bottom = y + h;
        for (int k = 0; k < 9; k++) {
            float f = (k + mod(t * 0.6f, 1f)) / 9f;
            int ly = hor + (int) ((bottom - hor) * f * f);
            g.fill(x, ly, x + w, ly + 1, al(0xFF2EE6, 0.55f));
        }
        for (int j = -8; j <= 8; j++)
            for (int py = hor; py < bottom; py += 3) {
                float f = (py - hor) / (float) (bottom - hor);
                int lx = cx + (int) (j * w * 0.09f * f * 1.6f);
                if (lx >= x && lx < x + w) g.fill(lx, py, lx + 1, py + 3, al(0xFF2EE6, 0.5f));
            }
        g.fill(x, hor, x + w, hor + 1, 0xFF5CE1FF);
    }

    // ---------- 3: Aurora ----------
    static void aurora(GuiGraphicsExtractor g, int x, int y, int w, int h, float t) {
        vgrad(g, x, y, w, h, 0xFF020611, 0xFF0A1A2A);
        stars(g, x, y, w, h, t, Math.max(20, w * h / 900));
        int[] cols = {0x40FF90, 0x40E0FF, 0xA060FF};
        for (int b = 0; b < 3; b++)
            for (int xi = 0; xi < w; xi += 4) {
                float cy = y + h * (0.22f + 0.13f * b) + (float) Math.sin(xi * 0.03 + t * 0.5 + b * 2) * h * 0.08f
                        + (float) Math.sin(xi * 0.011 - t * 0.3) * h * 0.04f;
                int top = (int) cy, th = (int) (h * 0.2f);
                int c = cols[b];
                fc(g, x + xi, top, x + xi + 4, top + th / 3, al(c, 0.28f), x, y, w, h);
                fc(g, x + xi, top + th / 3, x + xi + 4, top + 2 * th / 3, al(c, 0.14f), x, y, w, h);
                fc(g, x + xi, top + 2 * th / 3, x + xi + 4, top + th, al(c, 0.05f), x, y, w, h);
            }
    }

    // ---------- 4: Matrix ----------
    static void matrix(GuiGraphicsExtractor g, int x, int y, int w, int h, float t) {
        g.fill(x, y, x + w, y + h, 0xFF000A04);
        for (int c = 0; c < w / 8; c++) {
            float sp = 40 + r(c, 1) * 90;
            float head = mod(t * sp + r(c, 2) * h * 2, h + 80) - 40;
            int cx = x + c * 8 + 2;
            for (int i = 0; i < 10; i++) {
                int py = y + (int) head - i * 6;
                if (py < y || py + 5 > y + h) continue;
                float a = 1f - i / 10f;
                boolean on = r(c * 50 + i, (int) (t * 8)) > 0.2f;
                if (on) g.fill(cx, py, cx + 3, py + 5, i == 0 ? 0xFFD8FFD8 : al(0x20FF60, a * 0.8f));
            }
        }
    }

    // ---------- 5: Snow Night ----------
    static void snow(GuiGraphicsExtractor g, int x, int y, int w, int h, float t) {
        vgrad(g, x, y, w, h, 0xFF0A1530, 0xFF223A66);
        int n = Math.max(30, w * h / 800);
        for (int i = 0; i < n; i++) {
            float fx = mod(r(i, 1) * w + (float) Math.sin(t * 0.8 + i) * 8, w);
            float fy = mod(r(i, 2) * h + t * (14 + r(i, 3) * 26), h);
            int s = r(i, 4) > 0.8f ? 2 : 1;
            g.fill(x + (int) fx, y + (int) fy, x + (int) fx + s, y + (int) fy + s, al(0xFFFFFF, 0.5f + 0.5f * r(i, 5)));
        }
        for (int xi = 0; xi < w; xi += 2) {
            float hill = h * 0.12f + (float) Math.sin(xi * 0.02) * h * 0.04f + (float) Math.sin(xi * 0.05 + 1) * h * 0.02f;
            g.fill(x + xi, y + h - (int) hill, x + xi + 2, y + h, 0xFFE8F0FF);
        }
    }
}
