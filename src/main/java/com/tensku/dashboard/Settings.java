package com.tensku.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Settings {
    private Settings() {}

    public static class Data {
        public int theme = 1;                 // 0..5, see Themes.NAMES
        public String font = "SansSerif";     // any PC font family name
        public boolean textOn = false;
        public String text = "TENSKU";
        public int textSize = 28;
        public int textColor = 0x7CFF2E;      // RGB
        public int effect = 2;                // 0 none,1 shadow,2 rainbow,3 wave,4 pulse,5 glow
        public float tx = 50, ty = 12;        // position in % of the screen
        public boolean hudOn = true;
        public boolean charOn = true;
        public boolean charRight = false;
        public int dance = 0;                 // see Dancer.NAMES
    }

    public static Data d = new Data();
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("tensku-dashboard-settings.json"); }

    public static void load() {
        try {
            if (Files.exists(file())) {
                Data x = GSON.fromJson(Files.readString(file()), Data.class);
                if (x != null) d = x;
            }
        } catch (Exception ignored) { }
    }

    public static void save() {
        try { Files.writeString(file(), GSON.toJson(d)); } catch (Exception ignored) { }
    }
}
