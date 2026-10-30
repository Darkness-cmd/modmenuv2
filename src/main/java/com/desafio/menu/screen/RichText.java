package com.desafio.menu.screen;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Texto con resaltado: "Consigue [[Lingotes de Hierro]]." -> lo marcado sale en azul. */
public final class RichText {
    private RichText() {}

    public static MutableText parse(String s, int baseRgb, int hiRgb) {
        MutableText out = Text.empty();
        if (s == null) return out;
        int idx = 0;
        while (idx < s.length()) {
            int a = s.indexOf("[[", idx);
            int b = a < 0 ? -1 : s.indexOf("]]", a + 2);
            if (a < 0 || b < 0) { out.append(Text.literal(s.substring(idx)).styled(st -> st.withColor(baseRgb))); break; }
            if (a > idx) out.append(Text.literal(s.substring(idx, a)).styled(st -> st.withColor(baseRgb)));
            out.append(Text.literal(s.substring(a + 2, b)).styled(st -> st.withColor(hiRgb)));
            idx = b + 2;
        }
        return out;
    }
}
