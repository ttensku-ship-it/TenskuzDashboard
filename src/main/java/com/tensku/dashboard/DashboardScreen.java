package com.tensku.dashboard;

import com.tensku.dashboard.Tracker.Payment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.*;

public class DashboardScreen extends Screen {
    static final String[] TABS = {"Dashboard", "Payments", "Players", "Auctions", "Text", "Style"};
    static final String[] RANGES = {"24H", "7D", "30D", "ALL"};
    static final long[] MS = {86400000L, 7 * 86400000L, 30 * 86400000L, Long.MAX_VALUE};
    static final int[] SWATCH = {0x7CFF2E, 0x2EE6FF, 0xFF2E9A, 0xFFE05A, 0xA060FF, 0xFF7A2E, 0xFFFFFF, 0xFF4D6D};

    static final int GREEN = 0xFF7CFF2E, RED = 0xFFFF4D6D, UP = 0xFF4DFF88;
    static final int PANEL = 0xD00F0D1A, SIDE = 0xE608080D, CARD = 0xCC16142A, GRAY = 0xFF9A9AB0, WHITE = 0xFFFFFFFF;

    record Hit(int x, int y, int w, int h, Runnable run) {}

    final List<Hit> hits = new ArrayList<>();
    int tab = 0, range = 0, fontScroll = 0;
    int px, py, pw, ph;
    int listX, listY, listW, listH; // font list rect (for scrolling)
    float tiltX, tiltY;
    final long opened = System.currentTimeMillis();

    public DashboardScreen() { super(Component.literal("Tensku Dashboard")); }

    @Override
    protected void init() {
        pw = Math.min(width - 30, 460);
        ph = Math.min(height - 20, 270);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    void txt(GuiGraphicsExtractor g, String s, int x, int y, int color, boolean shadow) { g.text(font, s, x, y, color, shadow); }

    void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean on, Runnable run) {
        g.fill(x, y, x + w, y + h, on ? GREEN : CARD);
        txt(g, label, x + w / 2 - font.width(label) / 2, y + h / 2 - 4, on ? 0xFF000000 : 0xFFE0E0F0, false);
        hits.add(new Hit(x, y, w, h, run));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        hits.clear();
        float time = System.currentTimeMillis() / 1000f;

        // full-screen animated theme behind everything
        Themes.draw(g, 0, 0, width, height, Settings.d.theme, time);
        g.fill(0, 0, width, height, 0x40000000);

        float t = Math.min(1f, (System.currentTimeMillis() - opened) / 320f);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        tiltY += ((mx - width / 2f) / width * 7f - tiltY) * 0.12f;
        tiltX += (-(my - height / 2f) / height * 5f - tiltX) * 0.12f;
        float yDeg = tiltY + (1f - ease) * 60f;

        var pose = g.pose();
        Fx.begin(pose, width / 2f, height / 2f, yDeg, tiltX);
        Fx.layers(g, px, py, pw, ph, yDeg, tiltX, 5);

        g.fill(px, py, px + pw, py + ph, PANEL);
        g.fill(px, py, px + 90, py + ph, SIDE);
        g.outline(px, py, pw, ph, 0xFF2A2840);

        txt(g, "Tensku's", px + 10, py + 8, WHITE, true);
        txt(g, "Dashboard", px + 10, py + 19, GRAY, false);
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 38 + i * 18, idx = i;
            boolean on = i == tab;
            if (on) { g.fill(px + 6, ty, px + 84, ty + 14, 0xFF1E3A12); g.fill(px + 6, ty, px + 8, ty + 14, GREEN); }
            txt(g, TABS[i], px + 14, ty + 3, on ? WHITE : GRAY, true);
            hits.add(new Hit(px + 6, ty, 78, 14, () -> tab = idx));
        }

        txt(g, "TENSKU DASHBOARD - " + TABS[tab].toUpperCase(), px + 98, py + 9, WHITE, true);
        for (int i = 0; i < RANGES.length; i++) {
            int rx = px + pw - 124 + i * 30, ry = py + 5, idx = i;
            boolean on = i == range;
            g.fill(rx, ry, rx + 28, ry + 14, on ? GREEN : CARD);
            txt(g, RANGES[i], rx + 14 - font.width(RANGES[i]) / 2, ry + 3, on ? 0xFF000000 : GRAY, false);
            hits.add(new Hit(rx, ry, 28, 14, () -> range = idx));
        }
        g.fill(px + 90, py + 24, px + pw, py + 25, 0xFF2A2840);

        List<Payment> list = Tracker.inRange(MS[range]);
        int x0 = px + 98, y0 = py + 32, w = pw - 106, h = ph - 52;
        switch (tab) {
            case 0 -> drawDashboard(g, list, x0, y0, w, h, time);
            case 1 -> drawPayments(g, list, x0, y0, w, h);
            case 2 -> drawPlayers(g, list, x0, y0, w, h);
            case 3 -> drawAuctions(g, x0, y0, w, h);
            case 4 -> drawText(g, x0, y0, w, h, time);
            default -> drawStyle(g, x0, y0, w, h, time);
        }

        txt(g, "Display only. Data is read from your chat, nothing is sent to the server.",
                px + 98, py + ph - 11, 0xFF5A5A70, false);
        Fx.end(pose);
    }

    // ---------- Dashboard ----------
    void drawDashboard(GuiGraphicsExtractor g, List<Payment> list, int x, int y, int w, int h, float time) {
        double rec = Tracker.received(list), snt = Tracker.sent(list), net = rec - snt;
        String me = minecraft != null ? minecraft.getUser().getName() : "you";
        txt(g, "Welcome, " + me, x, y, UP, true);

        int cy = y + 12, cw = (w - 18) / 4;
        card(g, x, cy, cw, "BALANCE (EST.)", Tracker.money(Tracker.data.balance), WHITE);
        card(g, x + (cw + 6), cy, cw, "PROFIT", (net >= 0 ? "+" : "") + Tracker.money(net), net >= 0 ? UP : RED);
        card(g, x + (cw + 6) * 2, cy, cw, "RECEIVED", Tracker.money(rec), UP);
        card(g, x + (cw + 6) * 3, cy, cw, "SENT", Tracker.money(snt), RED);

        int by = cy + 38, bh = h - 50, fw = (int) (w * 0.42f);
        g.fill(x, by, x + fw, by + bh, CARD);
        txt(g, "PAYMENT FEED", x + 6, by + 5, WHITE, false);
        int rows = Math.max(0, (bh - 20) / 11);
        for (int i = 0; i < rows && i < list.size(); i++) {
            Payment p = list.get(list.size() - 1 - i);
            int ry = by + 18 + i * 11;
            txt(g, trim(p.name, 12), x + 6, ry, 0xFFE0E0F0, false);
            String a = (p.amount >= 0 ? "+" : "") + Tracker.money(p.amount);
            txt(g, a, x + fw - 6 - font.width(a), ry, p.amount >= 0 ? UP : RED, false);
        }
        if (list.isEmpty()) txt(g, "No payments yet", x + 6, by + 20, GRAY, false);

        int gx = x + fw + 6, gw = w - fw - 6;
        g.fill(gx, by, gx + gw, by + bh, CARD);
        txt(g, "BALANCE", gx + 6, by + 5, WHITE, false);
        String b = Tracker.money(Tracker.data.balance);
        txt(g, b, gx + gw - 6 - font.width(b), by + 5, WHITE, true);
        chart(g, list, gx + 6, by + 18, gw - 12, bh - 24);
    }

    void drawPayments(GuiGraphicsExtractor g, List<Payment> list, int x, int y, int w, int h) {
        double rec = Tracker.received(list), snt = Tracker.sent(list), net = rec - snt;
        int cw = (w - 18) / 4;
        card(g, x, y, cw, "RECEIVED", "+" + Tracker.money(rec), UP);
        card(g, x + (cw + 6), y, cw, "SENT", "-" + Tracker.money(snt), RED);
        card(g, x + (cw + 6) * 2, y, cw, "NET", (net >= 0 ? "+" : "") + Tracker.money(net), net >= 0 ? UP : RED);
        card(g, x + (cw + 6) * 3, y, cw, "TRANSACTIONS", String.valueOf(list.size()), WHITE);

        int ty = y + 38;
        g.fill(x, ty, x + w, y + h, CARD);
        txt(g, "PLAYER", x + 6, ty + 5, GRAY, false);
        txt(g, "AMOUNT", x + w / 2, ty + 5, GRAY, false);
        txt(g, "WHEN", x + w - 40, ty + 5, GRAY, false);
        int rows = Math.max(0, (h - 38 - 20) / 11);
        for (int i = 0; i < rows && i < list.size(); i++) {
            Payment p = list.get(list.size() - 1 - i);
            int ry = ty + 18 + i * 11;
            txt(g, trim(p.name, 14), x + 6, ry, 0xFFE0E0F0, false);
            txt(g, (p.amount >= 0 ? "+" : "") + Tracker.money(p.amount), x + w / 2, ry, p.amount >= 0 ? UP : RED, false);
            txt(g, Tracker.ago(p.time), x + w - 40, ry, GRAY, false);
        }
    }

    void drawPlayers(GuiGraphicsExtractor g, List<Payment> list, int x, int y, int w, int h) {
        Map<String, double[]> map = new HashMap<>();
        for (Payment p : list) {
            double[] a = map.computeIfAbsent(p.name, k -> new double[2]);
            a[0] += p.amount;
            a[1]++;
        }
        List<Map.Entry<String, double[]>> rows = new ArrayList<>(map.entrySet());
        rows.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]));

        txt(g, "Everyone you traded with. Top = paid you the most.", x, y, GRAY, false);
        int ty = y + 14;
        g.fill(x, ty, x + w, y + h, CARD);
        txt(g, "#  PLAYER", x + 6, ty + 5, GRAY, false);
        txt(g, "PAYMENTS", x + w / 2 - 10, ty + 5, GRAY, false);
        txt(g, "NET", x + w - 50, ty + 5, GRAY, false);
        int max = Math.max(0, (h - 14 - 20) / 11);
        for (int i = 0; i < max && i < rows.size(); i++) {
            var e = rows.get(i);
            int ry = ty + 18 + i * 11;
            txt(g, (i + 1) + "  " + trim(e.getKey(), 14), x + 6, ry, 0xFFE0E0F0, false);
            txt(g, String.valueOf((int) e.getValue()[1]), x + w / 2 - 10, ry, GRAY, false);
            double n = e.getValue()[0];
            String s = (n >= 0 ? "+" : "") + Tracker.money(n);
            txt(g, s, x + w - 6 - font.width(s), ry, n >= 0 ? UP : RED, false);
        }
    }

    // ---------- Auctions ----------
    void drawAuctions(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int half = (w - 6) / 2;
        g.fill(x, y, x + half, y + h, CARD);
        txt(g, "CURRENT AUCTION", x + 6, y + 5, WHITE, false);
        if (Auction.active) {
            long left = Math.max(0, Auction.endAt - System.currentTimeMillis());
            txt(g, Auction.item, x + 6, y + 18, UP, true);
            txt(g, (left / 1000) + "s left", x + 6, y + 30, GRAY, false);
            var top = Auction.top();
            for (int i = 0; i < Math.min(5, top.size()); i++)
                txt(g, (i + 1) + ". " + trim(top.get(i).getKey(), 12) + "  " + Tracker.money(top.get(i).getValue()),
                        x + 6, y + 44 + i * 11, 0xFFE0E0F0, false);
            if (top.isEmpty()) txt(g, "No bids yet", x + 6, y + 44, GRAY, false);
            button(g, x + 6, y + h - 36, half - 12, 14, "END NOW", true, Auction::finish);
            button(g, x + 6, y + h - 18, half - 12, 14, "CANCEL", false, Auction::cancel);
        } else {
            txt(g, "No auction running", x + 6, y + 18, GRAY, false);
            txt(g, "Start one in chat:", x + 6, y + 34, WHITE, false);
            txt(g, "/tauction start 30 Elytra", x + 6, y + 46, UP, false);
            txt(g, "Payments you get while it", x + 6, y + 62, GRAY, false);
            txt(g, "runs count as bids. Biggest", x + 6, y + 73, GRAY, false);
            txt(g, "single payment wins.", x + 6, y + 84, GRAY, false);
            button(g, x + 6, y + h - 18, half - 12, 14, "PREVIEW WIN ANIMATION", false, () -> { Auction.testFx(); minecraft.setScreenAndShow(null); });
        }

        int rx = x + half + 6;
        g.fill(rx, y, x + w, y + h, CARD);
        txt(g, "LAST AUCTIONS", rx + 6, y + 5, WHITE, false);
        if (Auction.history.isEmpty()) txt(g, "Nothing yet", rx + 6, y + 18, GRAY, false);
        for (int i = 0; i < Math.min(8, Auction.history.size()); i++) {
            var r = Auction.history.get(i);
            String s = r.winner == null ? trim(r.item, 10) + " > no bids"
                    : trim(r.item, 8) + " > " + trim(r.winner, 8) + " " + Tracker.money(r.amount);
            txt(g, s, rx + 6, y + 18 + i * 11, r.winner == null ? GRAY : UP, false);
        }
    }

    // ---------- Text (fonts) ----------
    void drawText(GuiGraphicsExtractor g, int x, int y, int w, int h, float time) {
        Settings.Data s = Settings.d;
        List<String> fonts = PixelText.fonts();
        listX = x; listY = y; listW = (int) (w * 0.46f); listH = h;
        g.fill(listX, listY, listX + listW, listY + listH, CARD);
        txt(g, "FONTS (" + fonts.size() + ") - scroll + click", listX + 6, listY + 4, GRAY, false);
        int rows = (listH - 18) / 11;
        fontScroll = Math.max(0, Math.min(Math.max(0, fonts.size() - rows), fontScroll));
        for (int i = 0; i < rows && fontScroll + i < fonts.size(); i++) {
            String f = fonts.get(fontScroll + i);
            int ry = listY + 16 + i * 11;
            boolean on = f.equals(s.font);
            if (on) g.fill(listX + 2, ry - 1, listX + listW - 2, ry + 10, 0xFF1E3A12);
            txt(g, trim(f, 26), listX + 6, ry, on ? UP : 0xFFE0E0F0, false);
            hits.add(new Hit(listX, ry - 1, listW, 11, () -> { s.font = f; Settings.save(); }));
        }

        int rx = x + listW + 8, rw = w - listW - 8;
        g.fill(rx, y, rx + rw, y + 52, CARD);
        PixelText.draw(g, s.text.isEmpty() ? "Tensku" : s.text, rx + rw / 2f, y + 26, Math.min(s.textSize, 26),
                s.textColor, s.effect, time, 1f);

        int by = y + 58;
        button(g, rx, by, rw, 14, "TEXT ON SCREEN: " + (s.textOn ? "ON" : "OFF"), s.textOn, () -> { s.textOn = !s.textOn; Settings.save(); });
        button(g, rx, by + 18, rw, 14, "EFFECT: " + PixelText.EFFECTS[s.effect], false,
                () -> { s.effect = (s.effect + 1) % PixelText.EFFECTS.length; Settings.save(); });
        int bw = (rw - 8) / 3;
        button(g, rx, by + 36, bw, 14, "SIZE -", false, () -> { s.textSize = Math.max(8, s.textSize - 4); Settings.save(); });
        g.fill(rx + bw + 4, by + 36, rx + 2 * bw + 4, by + 50, CARD);
        txt(g, String.valueOf(s.textSize), rx + bw + 4 + bw / 2 - font.width(String.valueOf(s.textSize)) / 2, by + 40, WHITE, false);
        button(g, rx + 2 * bw + 8, by + 36, bw, 14, "SIZE +", false, () -> { s.textSize = Math.min(96, s.textSize + 4); Settings.save(); });

        int sw = (rw - 7 * 3) / 8;
        for (int i = 0; i < SWATCH.length; i++) {
            int c = SWATCH[i], sx = rx + i * (sw + 3);
            g.fill(sx, by + 56, sx + sw, by + 68, 0xFF000000 | c);
            if (c == s.textColor) g.outline(sx - 1, by + 55, sw + 2, 14, WHITE);
            hits.add(new Hit(sx, by + 56, sw, 12, () -> { s.textColor = c; Settings.save(); }));
        }
        txt(g, "Change the words in chat:", rx, by + 74, GRAY, false);
        txt(g, "/ttext set Your words here", rx, by + 85, UP, false);
        txt(g, "/ttext pos 50 12  (x y in %)", rx, by + 96, UP, false);
    }

    // ---------- Style (themes + character) ----------
    void drawStyle(GuiGraphicsExtractor g, int x, int y, int w, int h, float time) {
        Settings.Data s = Settings.d;
        int cols = 3, cw = (w - (cols - 1) * 6) / cols, chh = (h - 50) / 2 - 2;
        for (int i = 0; i < Themes.NAMES.length; i++) {
            int cx = x + (i % cols) * (cw + 6), cy = y + (i / cols) * (chh + 4), idx = i;
            Themes.draw(g, cx, cy, cw, chh, i, time);
            g.fill(cx, cy + chh - 13, cx + cw, cy + chh, 0xBB000000);
            txt(g, Themes.NAMES[i], cx + 4, cy + chh - 11, i == s.theme ? UP : WHITE, true);
            if (i == s.theme) g.outline(cx - 1, cy - 1, cw + 2, chh + 2, GREEN);
            hits.add(new Hit(cx, cy, cw, chh, () -> { s.theme = idx; Settings.save(); }));
        }
        int by = y + 2 * (chh + 4) + 2, bw = (w - 6) / 2;
        button(g, x, by, bw, 14, "HUD PANEL: " + (s.hudOn ? "ON" : "OFF"), s.hudOn, () -> { s.hudOn = !s.hudOn; Settings.save(); });
        button(g, x + bw + 6, by, bw, 14, "CHARACTER: " + (s.charOn ? "ON" : "OFF"), s.charOn, () -> { s.charOn = !s.charOn; Settings.save(); });
        button(g, x, by + 18, bw, 14, "SIDE: " + (s.charRight ? "RIGHT" : "LEFT"), false, () -> { s.charRight = !s.charRight; Settings.save(); });
        button(g, x + bw + 6, by + 18, bw, 14, "DANCE: " + Dancer.NAMES[Math.floorMod(s.dance, Dancer.NAMES.length)], false,
                () -> { s.dance = (s.dance + 1) % Dancer.NAMES.length; Settings.save(); });
    }

    // ---------- widgets ----------
    void card(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, int color) {
        g.fill(x, y, x + w, y + 34, CARD);
        g.fill(x, y, x + 2, y + 34, color);
        txt(g, label, x + 6, y + 5, GRAY, false);
        txt(g, value, x + 6, y + 18, color, true);
    }

    void chart(GuiGraphicsExtractor g, List<Payment> list, int x, int y, int w, int h) {
        if (list.isEmpty() || w < 4 || h < 4) { txt(g, "No data yet", x, y + 4, GRAY, false); return; }
        double[] v = new double[Math.max(2, list.size())];
        for (int i = 0; i < list.size(); i++) v[i] = list.get(i).balanceAfter;
        if (list.size() == 1) v[1] = v[0];
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double d : v) { min = Math.min(min, d); max = Math.max(max, d); }
        if (max - min < 1) max = min + 1;

        int prev = -1;
        for (int i2 = 0; i2 < w; i2++) {
            double pos = i2 / (double) (w - 1) * (v.length - 1);
            int i = (int) Math.floor(pos);
            int j = Math.min(v.length - 1, i + 1);
            double val = v[i] + (v[j] - v[i]) * (pos - i);
            int yy = y + h - 1 - (int) ((val - min) / (max - min) * (h - 2));
            g.fill(x + i2, yy, x + i2 + 1, y + h, 0x3340FF60);
            int top = prev < 0 ? yy : Math.min(prev, yy);
            int bot = prev < 0 ? yy + 2 : Math.max(prev, yy) + 2;
            g.fill(x + i2, top, x + i2 + 1, bot, GREEN);
            prev = yy;
        }
    }

    String trim(String s, int n) { return s.length() > n ? s.substring(0, n) : s; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x && mx <= h.x + h.w && my >= h.y && my <= h.y + h.h) { h.run.run(); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (tab == 4 && mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            fontScroll -= (int) Math.signum(scrollY) * 3;
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
