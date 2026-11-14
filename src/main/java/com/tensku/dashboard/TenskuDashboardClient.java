package com.tensku.dashboard;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TenskuDashboardClient implements ClientModInitializer {
    static KeyMapping openKey, danceKey;

    @Override
    public void onInitializeClient() {
        Tracker.load();
        Settings.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("tensku_dashboard", "main"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tensku_dashboard.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        danceKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tensku_dashboard.dance", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) mc.setScreenAndShow(new DashboardScreen());
            while (danceKey.consumeClick()) { Settings.d.charOn = !Settings.d.charOn; Settings.save(); }
            Auction.tick();
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) Tracker.handle(message.getString());
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /tauction start <seconds> <item...> | end | cancel
            dispatcher.register(ClientCommandManager.literal("tauction")
                    .then(ClientCommandManager.literal("start")
                            .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(5, 600))
                                    .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                            .executes(c -> {
                                                Auction.start(IntegerArgumentType.getInteger(c, "seconds"),
                                                        StringArgumentType.getString(c, "item"));
                                                c.getSource().sendFeedback(Component.literal("Auction started!"));
                                                return 1;
                                            }))))
                    .then(ClientCommandManager.literal("end").executes(c -> { Auction.finish(); return 1; }))
                    .then(ClientCommandManager.literal("cancel").executes(c -> { Auction.cancel(); return 1; })));

            // /ttext ...
            dispatcher.register(ClientCommandManager.literal("ttext")
                    .then(ClientCommandManager.literal("set").then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                            .executes(c -> { Settings.d.text = StringArgumentType.getString(c, "text"); Settings.d.textOn = true; Settings.save(); return 1; })))
                    .then(ClientCommandManager.literal("on").executes(c -> { Settings.d.textOn = true; Settings.save(); return 1; }))
                    .then(ClientCommandManager.literal("off").executes(c -> { Settings.d.textOn = false; Settings.save(); return 1; }))
                    .then(ClientCommandManager.literal("size").then(ClientCommandManager.argument("n", IntegerArgumentType.integer(8, 96))
                            .executes(c -> { Settings.d.textSize = IntegerArgumentType.getInteger(c, "n"); Settings.save(); return 1; })))
                    .then(ClientCommandManager.literal("color").then(ClientCommandManager.argument("hex", StringArgumentType.word())
                            .executes(c -> {
                                try { Settings.d.textColor = Integer.parseInt(StringArgumentType.getString(c, "hex").replace("#", ""), 16) & 0xFFFFFF; Settings.save(); }
                                catch (Exception e) { c.getSource().sendFeedback(Component.literal("Use a hex color like ff00aa")); }
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("effect").then(ClientCommandManager.argument("n", IntegerArgumentType.integer(0, 5))
                            .executes(c -> { Settings.d.effect = IntegerArgumentType.getInteger(c, "n"); Settings.save(); return 1; })))
                    .then(ClientCommandManager.literal("pos")
                            .then(ClientCommandManager.argument("x", DoubleArgumentType.doubleArg(0, 100))
                                    .then(ClientCommandManager.argument("y", DoubleArgumentType.doubleArg(0, 100))
                                            .executes(c -> {
                                                Settings.d.tx = (float) DoubleArgumentType.getDouble(c, "x");
                                                Settings.d.ty = (float) DoubleArgumentType.getDouble(c, "y");
                                                Settings.save();
                                                return 1;
                                            }))))
                    .then(ClientCommandManager.literal("font").then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                            .executes(c -> { Settings.d.font = StringArgumentType.getString(c, "name"); Settings.save(); return 1; }))));

            dispatcher.register(ClientCommandManager.literal("ttheme")
                    .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(0, Themes.NAMES.length - 1))
                            .executes(c -> { Settings.d.theme = IntegerArgumentType.getInteger(c, "n"); Settings.save(); return 1; })));

            dispatcher.register(ClientCommandManager.literal("tdance")
                    .executes(c -> { Settings.d.charOn = !Settings.d.charOn; Settings.save(); return 1; })
                    .then(ClientCommandManager.argument("style", IntegerArgumentType.integer(0, Dancer.NAMES.length - 1))
                            .executes(c -> { Settings.d.dance = IntegerArgumentType.getInteger(c, "style"); Settings.d.charOn = true; Settings.save(); return 1; })));
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("tensku_dashboard", "overlay"),
                TenskuDashboardClient::renderHud);
    }

    // The always-on overlay: text, character, balance panel, auction timer, winner animation
    static void renderHud(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        float t = System.currentTimeMillis() / 1000f;

        PixelText.drawOverlay(g, W, H, t);
        if (mc.screen != null) return;

        if (Settings.d.charOn) {
            float s = 2f;
            int cx = Settings.d.charRight ? W - 30 : 30;
            Dancer.render(g, cx, H - 8 - (int) (32 * s) - 4, s);
        }

        if (Settings.d.hudOn) {
            List<Tracker.Payment> day = Tracker.inRange(86400000L);
            double profit = Tracker.received(day) - Tracker.sent(day);
            int x = Settings.d.charRight ? 8 : 8, y = 8, w = 124, h = 42;
            float yaw = (float) Math.sin(t / 1.5) * 22f;

            var pose = g.pose();
            Fx.begin(pose, x + w / 2f, y + h / 2f, yaw, 0f);
            pose.pushMatrix();
            pose.translate(x, y);
            Fx.layers(g, 0, 0, w, h, yaw, 0f, 4);
            Themes.draw(g, 0, 0, w, h, Settings.d.theme, t);
            g.fill(0, 0, w, h, 0x99000000);
            g.outline(0, 0, w, h, 0xFF7CFF2E);
            g.text(mc.font, "TENSKU", 6, 5, 0xFF9A9AB0, true);
            g.text(mc.font, Tracker.money(Tracker.data.balance), 6, 17, 0xFFFFFFFF, true);
            g.text(mc.font, (profit >= 0 ? "+" : "") + Tracker.money(profit) + " today", 6, 29,
                    profit >= 0 ? 0xFF4DFF88 : 0xFFFF4D6D, true);
            pose.popMatrix();
            Fx.end(pose);
        }

        Auction.renderHud(g, W, H, t);
        Auction.renderFx(g, W, H, t);
    }
}
