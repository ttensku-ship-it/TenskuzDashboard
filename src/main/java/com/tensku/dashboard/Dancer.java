package com.tensku.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/** Your own Minecraft skin, drawn flat and animated so it dances on the side of your HUD. */
public final class Dancer {
    private Dancer() {}

    public static final String[] NAMES = {"Bounce", "Disco", "Wave", "Spin", "Floss"};
    static float boost = 0;
    static long last = System.currentTimeMillis();

    public static void celebrate(float amount) { boost = Math.min(1.6f, boost + amount); }

    static Identifier skin(Minecraft mc) {
        return mc.player.getSkin().body().texturePath(); // if this line errors on 26.3, paste the error to me
    }

    static void part(GuiGraphicsExtractor g, Identifier tex, int u, int v, int w, int h,
                     float px, float py, float qx, float qy, float s, float ang) {
        Matrix3x2fStack p = g.pose();
        p.pushMatrix();
        p.translate(px * s, py * s);
        p.rotate(ang);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, Math.round(-qx * s), Math.round(-qy * s),
                (float) u, (float) v, Math.round(w * s), Math.round(h * s), w, h, 64, 64);
        p.popMatrix();
    }

    /** x,y = top-center of the character. scale = pixels per skin pixel. */
    public static void render(GuiGraphicsExtractor g, int x, int y, float s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        boost = Math.max(0, boost - (now - last) / 1000f * 0.25f);
        last = now;

        Identifier tex = skin(mc);
        float t = now / 1000f;
        float ph = t * (1f + boost * 1.5f) * 6.28f * 1.5f;
        int style = Math.floorMod(Settings.d.dance, NAMES.length);

        float bob = 0, sway = 0, sx = 1, armL = 0, armR = 0, legL = 0, legR = 0, head = 0, shift = 0;
        float sn = (float) Math.sin(ph), cs = (float) Math.cos(ph);
        switch (style) {
            case 0 -> { bob = Math.abs(sn) * 3; armL = 0.7f + 0.8f * sn; armR = 0.7f - 0.8f * sn; legL = 0.2f * sn; legR = -0.2f * sn; head = 0.08f * sn; }
            case 1 -> { bob = Math.abs(sn) * 1.5f; sway = sn * 0.1f; armL = 2.6f + 0.25f * (float) Math.sin(ph * 2); armR = 0.35f; legL = 0.3f * sn; legR = -0.3f * sn; head = 0.1f * cs; }
            case 2 -> { bob = Math.abs(cs) * 2; armL = 1.3f * sn + 0.8f; armR = -1.3f * sn + 0.8f; legL = 0.4f * sn; legR = -0.4f * sn; head = 0.12f * sn; }
            case 3 -> { bob = Math.abs(sn) * 2; sx = Math.max(0.15f, Math.abs((float) Math.cos(ph * 0.5f))); armL = 1.5f; armR = 1.5f; legL = 0.15f * sn; legR = -0.15f * sn; }
            default -> { bob = Math.abs(sn) * 1.5f; sway = 0.18f * sn; shift = 3 * sn; armL = 0.5f + 0.5f * (float) Math.sin(ph * 2); armR = 0.5f - 0.5f * (float) Math.sin(ph * 2); head = -0.1f * sn; }
        }

        Matrix3x2fStack p = g.pose();
        // floor glow
        g.fill(x - (int) (11 * s), y + (int) (32 * s), x + (int) (11 * s), y + (int) (32 * s) + 2, 0x557CFF2E);
        g.fill(x - (int) (8 * s), y + (int) (32 * s) + 2, x + (int) (8 * s), y + (int) (32 * s) + 3, 0x337CFF2E);

        p.pushMatrix();
        p.translate(x + shift * s * 0.5f, y - bob * s * 0.5f);
        p.scale(sx, 1f);
        p.translate(0, 20 * s);
        p.rotate(sway);
        p.translate(0, -20 * s);

        // character-right limbs are drawn on the screen-left. positive angle raises screen-left limb outward.
        part(g, tex, 4, 20, 4, 12, -2, 20, 2, 0, s, legR);          // right leg
        part(g, tex, 20, 52, 4, 12, 2, 20, 2, 0, s, -legL);         // left leg
        part(g, tex, 20, 20, 8, 12, 0, 8, 4, 0, s, 0f);             // body
        part(g, tex, 44, 20, 4, 12, -6, 8, 2, 0, s, armL);          // right arm
        part(g, tex, 36, 52, 4, 12, 6, 8, 2, 0, s, -armR);          // left arm
        part(g, tex, 8, 8, 8, 8, 0, 8, 4, 8, s, head);              // head
        part(g, tex, 40, 8, 8, 8, 0, 8, 4, 8, s, head);             // hat layer
        p.popMatrix();
    }
}
