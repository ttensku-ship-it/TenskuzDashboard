package com.tensku.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import java.util.*;

/** Auctions: payments you receive while one runs count as bids (biggest single payment wins). */
public final class Auction {
    private Auction() {}

    public static class Result {
        public String item, winner;
        public double amount;
        public long time;
    }

    public static boolean active;
    public static String item = "";
    public static long startAt, endAt;
    public static final Map<String, Double> bids = new HashMap<>();
    public static final List<Result> history = new ArrayList<>();

    static long fxStart = 0;
    static Result fxResult;
    static final long FX_MS = 7000;

    public static void start(int seconds, String name) {
        active = true;
        item = name;
        startAt = System.currentTimeMillis();
        endAt = startAt + seconds * 1000L;
        bids.clear();
    }

    public static void cancel() { active = false; bids.clear(); }

    public static void onPayment(String name, double amount) {
        if (active) bids.merge(name, amount, Math::max);
    }

    public static void tick() {
        if (active && System.currentTimeMillis() >= endAt) finish();
    }

    public static void finish() {
        if (!active) return;
        active = false;
        Result res = new Result();
        res.item = item;
        res.time = System.currentTimeMillis();
        for (var e : bids.entrySet())
            if (res.winner == null || e.getValue() > res.amount) { res.winner = e.getKey(); res.amount = e.getValue(); }
        history.add(0, res);
        if (history.size() > 10) history.remove(history.size() - 1);
        playFx(res);
    }

    public static void playFx(Result res) {
        fxResult = res;
        fxStart = System.currentTimeMillis();
        if (res.winner != null) Dancer.celebrate(1.6f);
    }

    public static void testFx() {
        Result r = new Result();
        r.item = "Elytra"; r.winner = "SomePlayer"; r.amount = 330_000_000; r.time = System.currentTimeMillis();
        playFx(r);
    }

    public static List<Map.Entry<String, Double>> top() {
        List<Map.Entry<String, Double>> l = new ArrayList<>(bids.entrySet());
        l.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return l;
    }

    // ---------- HUD: ring timer ----------
    public static void renderHud(GuiGraphicsExtractor g, int W, int H, float t) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        long left = Math.max(0, endAt - System.currentTimeMillis());
        float frac = left / (float) Math.max(1, endAt - startAt);
        int cx = W / 2, cy = (int) (H * 0.32f), rad = 30;
        float yaw = (float) Math.sin(t * 1.2) * 14f;

        Matrix3x2fStack p = g.pose();
        Fx.begin(p, cx, cy, yaw, 0f);
        g.fill(cx - 70, cy - 52, cx + 70, cy + 52, 0xAA0F0D1A);
        g.outline(cx - 70, cy - 52, 140, 104, 0xFF7CFF2E);
        PixelText.draw(g, item.isEmpty() ? "AUCTION" : item, cx, cy - 40, 16, 0xFFFFFF, 1, t, 1f);
        int n = 72;
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + i / (double) n * Math.PI * 2;
            int dx = cx + (int) Math.round(Math.cos(a) * rad), dy = cy + (int) Math.round(Math.sin(a) * rad);
            boolean lit = i < frac * n;
            g.fill(dx - 1, dy - 1, dx + 2, dy + 2, lit ? (frac < 0.25f ? 0xFFFF4D6D : 0xFF5B8CFF) : 0x30FFFFFF);
        }
        String secs = String.valueOf((int) Math.ceil(left / 1000.0));
        g.text(mc.font, secs, cx - mc.font.width(secs) / 2, cy - 4, 0xFFFFFFFF, true);
        var top = top();
        String line = top.isEmpty() ? "No bids yet" : top.get(0).getKey() + "  " + Tracker.money(top.get(0).getValue());
        g.text(mc.font, line, cx - mc.font.width(line) / 2, cy + 38, top.isEmpty() ? 0xFF9A9AB0 : 0xFF4DFF88, true);
        Fx.end(p);
    }

    // ---------- HUD: winner animation after the auction ends ----------
    static float ease(float p) { // easeOutBack
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(p - 1, 3) + c1 * (float) Math.pow(p - 1, 2);
    }

    public static void renderFx(GuiGraphicsExtractor g, int W, int H, float time) {
        if (fxResult == null) return;
        long el = System.currentTimeMillis() - fxStart;
        if (el > FX_MS) { fxResult = null; return; }
        float t = el / 1000f, total = FX_MS / 1000f;
        float fade = Math.min(1f, Math.min(t / 0.3f, (total - t) / 0.8f));
        boolean won = fxResult.winner != null;
        Minecraft mc = Minecraft.getInstance();

        g.fill(0, 0, W, H, Themes.al(0x000000, 0.45f * fade));
        if (t < 0.25f) g.fill(0, 0, W, H, Themes.al(0xFFFFFF, 0.6f * (1 - t / 0.25f)));

        if (won) {
            // expanding rings
            if (t < 1.4f) {
                int cxr = W / 2, cyr = (int) (H * 0.38f);
                for (int ring = 0; ring < 3; ring++) {
                    float tt = t - ring * 0.18f;
                    if (tt < 0) continue;
                    float rr = tt * H * 0.55f, a = Math.max(0, 1 - tt / 1.2f);
                    for (int i = 0; i < 64; i++) {
                        double an = i / 64.0 * Math.PI * 2;
                        int px = cxr + (int) (Math.cos(an) * rr), py = cyr + (int) (Math.sin(an) * rr);
                        g.fill(px - 1, py - 1, px + 2, py + 2, Themes.al(0xFFE05A, a));
                    }
                }
            }
            // confetti
            int[] pal = {0xFF2E9A, 0x2EE6FF, 0xFFE05A, 0x7CFF2E, 0xA060FF, 0xFF7A2E};
            for (int i = 0; i < 150; i++) {
                float sp = 40 + Themes.r(i, 2) * 90;
                float y = -10 + sp * t - Themes.r(i, 4) * H * 0.4f;
                float x = Themes.r(i, 1) * W + (float) Math.sin(t * 2 + Themes.r(i, 5) * 6) * (10 + Themes.r(i, 6) * 18);
                if (y < 0 || y > H) continue;
                int s = 2 + (int) (Themes.r(i, 7) * 3);
                boolean flip = ((int) (t * 6 + i)) % 2 == 0;
                g.fill((int) x, (int) y, (int) x + (flip ? s : s + 2), (int) y + (flip ? s + 2 : s),
                        Themes.al(pal[i % pal.length], fade));
            }
        }

        // big text with pop-in
        float pop = Math.min(1f, t / 0.55f);
        float sc = ease(pop);
        Matrix3x2fStack p = g.pose();
        p.pushMatrix();
        p.translate(W / 2f, H * 0.38f);
        p.scale(sc, sc);
        PixelText.draw(g, won ? "SOLD!" : "NO BIDS", 0, 0, 56, won ? 0xFFE05A : 0x9A9AB0, won ? 2 : 1, time, fade);
        p.popMatrix();

        float slide = Math.min(1f, Math.max(0f, (t - 0.45f) / 0.5f));
        if (slide > 0) {
            float off = (1 - slide) * 30;
            if (won) {
                PixelText.draw(g, fxResult.winner, W / 2f, H * 0.38f + 52 + off, 30, 0xFFFFFF, 5, time, fade * slide);
                PixelText.draw(g, "won " + fxResult.item + " for " + Tracker.money(fxResult.amount),
                        W / 2f, H * 0.38f + 82 + off, 16, 0x7CFF2E, 1, time, fade * slide);
            } else {
                PixelText.draw(g, fxResult.item, W / 2f, H * 0.38f + 52 + off, 20, 0xFFFFFF, 1, time, fade * slide);
            }
        }
    }
}
