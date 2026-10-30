package com.desafio.menu.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/** Paleta y utilidades de dibujo (colores medidos del video). */
public final class Ui {
    public static final int CYAN = 0xFF55FFFF;
    public static final int GOLD = 0xFFFFAA00;
    public static final int RED = 0xFFFF5555;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GRAY = 0xFF8A93A0;
    public static final int BLUE_HI = 0x5555FF;      // texto resaltado [[...]]
    public static final int BG_PANEL = 0xFF04080F;
    public static final int BG_SLOT = 0xFF0F1626;
    public static final int BORDER_SLOT = 0xFF2A3A55;
    public static final int SELECT = 0xFF22E3F0;     // borde cian de la pestaña activa
    public static final int BAR_FILL = 0xFF3B82F6;
    public static final int BAR_BACK = 0xFF141A24;

    private static final Map<Identifier, Boolean> EXISTS = new HashMap<>();

    private Ui() {}

    public static boolean exists(Identifier id) {
        return EXISTS.computeIfAbsent(id, i -> MinecraftClient.getInstance().getResourceManager().getResource(i).isPresent());
    }

    public static void clearCache() { EXISTS.clear(); }

    public static int rainbow(float t) {
        return 0xFF000000 | MathHelper.hsvToRgb(t - (float) Math.floor(t), 0.85f, 1f);
    }

    public static void rainbowText(DrawContext c, TextRenderer tr, String s, int x, int y) {
        float t = Util.getMeasuringTimeMs() / 2500f;
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            c.drawText(tr, ch, cx, y, rainbow(i * 0.13f + t), true);
            cx += tr.getWidth(ch);
        }
    }

    public static void border(DrawContext c, int x1, int y1, int x2, int y2, int color) {
        c.fill(x1, y1, x2, y1 + 1, color);
        c.fill(x1, y2 - 1, x2, y2, color);
        c.fill(x1, y1, x1 + 1, y2, color);
        c.fill(x2 - 1, y1, x2, y2, color);
    }

    /** Ítem dibujado a 'px' píxeles de lado. */
    public static void item(DrawContext c, ItemStack s, int x, int y, int px) {
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        float sc = px / 16f;
        m.scale(sc, sc, 1f);
        c.drawItem(s, 0, 0);
        m.pop();
    }

    public static void rainbowBarH(DrawContext c, int x1, int x2, int y1, int y2) {
        int w = Math.max(1, x2 - x1);
        for (int x = x1; x < x2; x++) c.fill(x, y1, x + 1, y2, rainbow((x - x1) / (float) w));
    }

    public static void rainbowBarV(DrawContext c, int x1, int x2, int y1, int y2) {
        int h = Math.max(1, y2 - y1);
        for (int y = y1; y < y2; y++) c.fill(x1, y, x2, y + 1, rainbow((y - y1) / (float) h));
    }
}
