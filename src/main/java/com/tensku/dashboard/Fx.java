package com.tensku.dashboard;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/** Fake 3D for the 2D GUI: squeeze the panel like it is turning, and stack shifted layers for thickness. */
final class Fx {
    private Fx() {}

    static void begin(Matrix3x2fStack p, float cx, float cy, float yDeg, float xDeg) {
        p.pushMatrix();
        p.translate(cx, cy);
        p.scale((float) Math.cos(Math.toRadians(yDeg)), (float) Math.cos(Math.toRadians(xDeg)));
        p.translate(-cx, -cy);
    }

    static void end(Matrix3x2fStack p) {
        p.popMatrix();
    }

    static void layers(GuiGraphicsExtractor g, int x, int y, int w, int h, float yDeg, float xDeg, int n) {
        float sx = (float) Math.sin(Math.toRadians(yDeg));
        float sy = (float) Math.sin(Math.toRadians(xDeg));
        Matrix3x2fStack p = g.pose();
        for (int i = n; i >= 1; i--) {
            p.pushMatrix();
            p.translate(i * (1.2f + sx * 3f), i * (1.2f - sy * 3f));
            int green = 0x34 - i * 6;
            g.fill(x, y, x + w, y + h, 0xFF000000 | (green << 8));
            p.popMatrix();
        }
    }
}
