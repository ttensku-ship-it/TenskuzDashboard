package com.tensku.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads payments + balance from chat, keeps them, saves them to config/tensku-dashboard.json */
public final class Tracker {
    private Tracker() {}

    public static class Payment {
        public String name;
        public double amount;        // + received, - sent
        public long time;
        public double balanceAfter;
    }

    public static class Data {
        public double balance = 0;
        public List<Payment> payments = new ArrayList<>();
    }

    public static Data data = new Data();
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Chat patterns. If your server words them differently, edit these three lines.
    static final String NUM = "\\$([\\d,]+(?:\\.\\d+)?)\\s*([KMBTkmbt]?)";
    static final Pattern IN = Pattern.compile("(\\w{3,16}) paid you " + NUM);
    static final Pattern OUT = Pattern.compile("You paid (\\w{3,16}) " + NUM);
    static final Pattern BAL = Pattern.compile("[Bb]alance:?\\s*" + NUM);

    public static void handle(String text) {
        Matcher m = IN.matcher(text);
        if (m.find()) { add(m.group(1), parse(m.group(2), m.group(3))); return; }
        m = OUT.matcher(text);
        if (m.find()) { add(m.group(1), -parse(m.group(2), m.group(3))); return; }
        m = BAL.matcher(text);
        if (m.find()) { data.balance = parse(m.group(1), m.group(2)); save(); }
    }

    static void add(String name, double amt) {
        data.balance += amt;
        Payment p = new Payment();
        p.name = name;
        p.amount = amt;
        p.time = System.currentTimeMillis();
        p.balanceAfter = data.balance;
        data.payments.add(p);
        if (amt > 0) {
            Auction.onPayment(name, amt);
            Dancer.celebrate(amt >= 1e9 ? 1.2f : 0.5f);
        }
        save();
    }

    static double parse(String num, String suffix) {
        double v = Double.parseDouble(num.replace(",", ""));
        return switch (suffix.toUpperCase()) {
            case "K" -> v * 1e3;
            case "M" -> v * 1e6;
            case "B" -> v * 1e9;
            case "T" -> v * 1e12;
            default -> v;
        };
    }

    public static List<Payment> inRange(long ms) {
        long now = System.currentTimeMillis();
        List<Payment> out = new ArrayList<>();
        for (Payment p : data.payments) if (ms == Long.MAX_VALUE || now - p.time <= ms) out.add(p);
        return out;
    }

    public static double received(List<Payment> l) { double s = 0; for (Payment p : l) if (p.amount > 0) s += p.amount; return s; }
    public static double sent(List<Payment> l) { double s = 0; for (Payment p : l) if (p.amount < 0) s -= p.amount; return s; }

    static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("tensku-dashboard.json"); }

    public static void load() {
        try {
            if (Files.exists(file())) {
                Data d = GSON.fromJson(Files.readString(file()), Data.class);
                if (d != null) data = d;
            }
        } catch (Exception ignored) { }
    }

    static void save() {
        try { Files.writeString(file(), GSON.toJson(data)); } catch (Exception ignored) { }
    }

    public static String money(double v) {
        String sign = v < 0 ? "-" : "";
        double a = Math.abs(v);
        String s;
        if (a >= 1e12) s = String.format("%.2fT", a / 1e12);
        else if (a >= 1e9) s = String.format("%.2fB", a / 1e9);
        else if (a >= 1e6) s = String.format("%.2fM", a / 1e6);
        else if (a >= 1e3) s = String.format("%.1fK", a / 1e3);
        else s = String.format("%.0f", a);
        return sign + "$" + s;
    }

    public static String ago(long t) {
        long s = (System.currentTimeMillis() - t) / 1000;
        if (s < 60) return s + "s ago";
        if (s < 3600) return (s / 60) + "m ago";
        if (s < 86400) return (s / 3600) + "h ago";
        return (s / 86400) + "d ago";
    }
}
