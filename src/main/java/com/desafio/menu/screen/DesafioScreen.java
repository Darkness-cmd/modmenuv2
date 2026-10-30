package com.desafio.menu.screen;

import com.desafio.menu.DesafioMenu;
import com.desafio.menu.client.ClientData;
import com.desafio.menu.client.DesafioMenuClient;
import com.desafio.menu.data.Content;
import com.desafio.menu.data.ItemSpec;
import com.desafio.menu.data.QuestLogic;
import com.desafio.menu.data.QuestLogic.State;
import com.desafio.menu.networking.ClaimPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/**
 * Menú principal. Todas las medidas están tomadas del video (1280x674) y se escalan a la ventana:
 * X(vx)/Y(vy) convierten "píxeles de video" a píxeles de pantalla.
 *
 * Barra lateral (orden del video): 0 Historia · 1 Misiones · 2 Guía · 3 Bestiario.
 */
public class DesafioScreen extends Screen implements ClientData.Refreshable {

    public enum Tab {
        LORE("Historia", Items.WRITABLE_BOOK),
        QUESTS("Misiones", Items.DIAMOND_SWORD),
        GUIDE("Guía", Items.PAPER),
        BESTIARY("Bestiario", Items.SPYGLASS);

        final String label;
        final Item fallback;
        final Identifier texture;

        Tab(String label, Item fallback) {
            this.label = label;
            this.fallback = fallback;
            this.texture = Identifier.of(DesafioMenu.MOD_ID, "textures/gui/tab_" + name().toLowerCase(Locale.ROOT) + ".png");
        }
    }

    private record Row(String id, String title, String sub, ItemStack icon, int goal, int progress, State state, boolean locked, boolean secret) {}

    // Rectángulos de los 4 botones de la barra lateral (x1,y1,x2,y2 en px de video)
    private static final int[][] SLOT = {{62, 45, 200, 178}, {62, 195, 200, 325}, {62, 355, 200, 485}, {62, 505, 200, 635}};
    private static final Identifier FRAME_TEX = Identifier.of(DesafioMenu.MOD_ID, "textures/gui/frame.png");

    private Tab tab = Tab.QUESTS;
    private String selectedId = null;
    private boolean mobDetail = false;
    private float scroll = 0;
    private String searchText = "";
    private TextFieldWidget search;
    private Text tipText = null;
    private ItemStack tipStack = ItemStack.EMPTY;
    private final Map<String, LivingEntity> entities = new HashMap<>();

    public DesafioScreen() { super(Text.literal("Desafío")); }

    // ------------------------------------------------------------------ helpers
    private int X(float vx) { return Math.round(vx * width / 1280f); }
    private int Y(float vy) { return Math.round(vy * height / 674f); }
    private static boolean in(double mx, double my, int x1, int y1, int x2, int y2) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    private int fh() { return textRenderer.fontHeight; }

    private boolean canEdit() {
        return client != null && client.player != null && (client.player.hasPermissionLevel(2) || client.isInSingleplayer());
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void refresh() { clearAndInit(); }

    // ------------------------------------------------------------------ init
    @Override
    protected void init() {
        Ui.clearCache();
        if (selectedId == null && tab != Tab.BESTIARY) {
            List<Row> rs = rows();
            if (!rs.isEmpty()) selectedId = rs.get(0).id;
        }

        search = new TextFieldWidget(textRenderer, X(245), Y(52), X(735) - X(245), fh() + 4, Text.empty());
        search.setDrawsBackground(false);
        search.setMaxLength(40);
        search.setPlaceholder(Text.literal("Buscar...").formatted(Formatting.DARK_GRAY));
        search.setText(searchText);   // antes del listener: si no, reinicia el scroll en cada refresco
        search.setChangedListener(s -> { searchText = s; scroll = 0; });
        if (!(tab == Tab.BESTIARY && mobDetail)) addDrawableChild(search);

        if (canEdit()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Editor"), b -> client.setScreen(new EditorScreen(this)))
                    .dimensions(X(1104), Y(600), X(1237) - X(1104), Y(32)).build());
        }
        if (tab == Tab.BESTIARY && mobDetail) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Volver"), b -> { mobDetail = false; scroll = 0; clearAndInit(); })
                    .dimensions(X(262), Y(551), X(407) - X(262), Y(36)).build());
        }
        if (tab == Tab.QUESTS) {
            Content.Quest q = quest(selectedId);
            if (q != null) {
                State st = QuestLogic.state(ClientData.me, q);
                String label = switch (st) { case LOCKED -> "Bloqueada"; case ACTIVE -> "En progreso"; case READY -> "Reclamar"; case CLAIMED -> "Completada"; };
                ButtonWidget btn = ButtonWidget.builder(Text.literal(label), b -> ClientPlayNetworking.send(new ClaimPayload(q.id)))
                        .dimensions(X(863), Y(497), X(1140) - X(863), Y(34)).build();
                btn.active = st == State.READY;
                addDrawableChild(btn);
            }
        }
    }

    private Content.Quest quest(String id) {
        if (id == null) return null;
        for (Content.Quest q : ClientData.content.quests) if (q.id.equals(id)) return q;
        return null;
    }

    private void switchTab(Tab t) {
        tab = t; selectedId = null; mobDetail = false; scroll = 0; searchText = "";
        clearAndInit();
    }

    // ------------------------------------------------------------------ filas
    private List<Row> rows() {
        String f = searchText.toLowerCase(Locale.ROOT);
        List<Row> out = new ArrayList<>();
        Content c = ClientData.content;
        switch (tab) {
            case QUESTS -> {
                for (Content.Quest q : c.quests) {
                    if (!q.title.toLowerCase(Locale.ROOT).contains(f)) continue;
                    State s = QuestLogic.state(ClientData.me, q);
                    out.add(new Row(q.id, q.title, q.description, ItemSpec.parse(q.icon), q.goal,
                            ClientData.me.progress.getOrDefault(q.id, 0), s, s == State.LOCKED, false));
                }
            }
            case GUIDE -> {
                for (Content.Guide g : c.guide) {
                    if (!g.title.toLowerCase(Locale.ROOT).contains(f)) continue;
                    out.add(new Row(g.id, g.title, g.text, ItemSpec.parse(g.icon), 0, 0, State.ACTIVE, false, false));
                }
            }
            case LORE -> {
                for (Content.Lore l : c.lore) {
                    boolean un = QuestLogic.loreUnlocked(ClientData.me, l);
                    if (un && !l.title.toLowerCase(Locale.ROOT).contains(f)) continue;
                    out.add(new Row(l.id, un ? l.title : "?????", un ? l.text : "", ItemSpec.parse(l.icon), 0, 0, State.ACTIVE, !un, !un));
                }
            }
            default -> {}
        }
        return out;
    }

    // ------------------------------------------------------------------ render
    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0xB0000000);   // sin blur: evita el límite de "un blur por frame"
        drawFrame(c);
        drawSidebar(c, mx, my);
        tipText = null;
        tipStack = ItemStack.EMPTY;
        switch (tab) {
            case QUESTS -> { drawList(c, rows(), mx, my, true); drawQuestDetail(c, mx, my); }
            case GUIDE, LORE -> { drawList(c, rows(), mx, my, false); drawTextDetail(c, mx, my); }
            case BESTIARY -> { if (mobDetail) drawMobDetail(c, mx, my); else drawMobGrid(c, mx, my); }
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);   // llama a renderBackground y luego dibuja los widgets
        if (!tipStack.isEmpty()) c.drawItemTooltip(textRenderer, tipStack, mx, my);
        else if (tipText != null) c.drawTooltip(textRenderer, tipText, mx, my);
    }

    // ---- marco metálico con tiras arcoíris
    private void drawFrame(DrawContext c) {
        if (Ui.exists(FRAME_TEX)) {
            c.drawTexture(FRAME_TEX, 0, 0, width, height, 0, 0, 1280, 674, 1280, 674);
            return;
        }
        c.fill(X(8), Y(8), X(1272), Y(666), 0xFF8E959F);
        c.fill(X(12), Y(12), X(1268), Y(662), 0xFFD6DBE2);
        c.fill(X(24), Y(24), X(1256), Y(650), 0xFF3A404A);
        c.fill(X(30), Y(28), X(1250), Y(646), Ui.BG_PANEL);
        // pestaña superior
        c.fill(X(528), Y(4), X(752), Y(32), 0xFF8E959F);
        c.fill(X(532), Y(6), X(748), Y(28), 0xFFD6DBE2);
        Ui.rainbowBarH(c, X(590), X(692), Y(16), Y(24));
        // pestaña inferior
        c.fill(X(450), Y(636), X(830), Y(670), 0xFF8E959F);
        c.fill(X(454), Y(640), X(826), Y(666), 0xFFD6DBE2);
        Ui.rainbowBarH(c, X(500), X(600), Y(647), Y(655));
        Ui.rainbowBarH(c, X(690), X(780), Y(647), Y(655));
        c.fill(X(620), Y(646), X(660), Y(656), 0xFF5CE8F0);
        // tiras laterales
        Ui.rainbowBarV(c, X(12), X(22), Y(250), Y(430));
        Ui.rainbowBarV(c, X(1258), X(1268), Y(250), Y(430));
        c.fill(X(12), Y(560), X(22), Y(620), 0xFF5CE8F0);
        c.fill(X(1258), Y(560), X(1268), Y(620), 0xFF5CE8F0);
    }

    private void drawSidebar(DrawContext c, int mx, int my) {
        Tab[] tabs = Tab.values();
        for (int i = 0; i < 4; i++) {
            int[] s = SLOT[i];
            int x1 = X(s[0]), y1 = Y(s[1]), x2 = X(s[2]), y2 = Y(s[3]);
            boolean sel = tabs[i] == tab, hov = in(mx, my, x1, y1, x2, y2);
            c.fill(x1, y1, x2, y2, 0xFF9AA1AB);
            c.fill(x1 + 3, y1 + 3, x2 - 3, y2 - 3, hov ? 0xFF121A28 : 0xFF0B111B);
            if (Ui.exists(tabs[i].texture)) {
                c.drawTexture(tabs[i].texture, x1, y1, x2 - x1, y2 - y1, 0, 0, 128, 128, 128, 128);
            } else {
                int px = Math.min(x2 - x1, y2 - y1) * 3 / 5;
                Ui.item(c, new ItemStack(tabs[i].fallback), (x1 + x2) / 2 - px / 2, (y1 + y2) / 2 - px / 2, px);
            }
            if (sel) {
                int t = Math.max(2, X(3));
                c.fill(x1 - t, y1 - t, x2 + t, y1, Ui.SELECT);
                c.fill(x1 - t, y2, x2 + t, y2 + t, Ui.SELECT);
                c.fill(x1 - t, y1, x1, y2, Ui.SELECT);
                c.fill(x2, y1, x2 + t, y2, Ui.SELECT);
            }
            if (hov) tipText = Text.literal(tabs[i].label);
        }
    }

    // ---- lista izquierda (misiones / guía / historia)
    private void drawList(DrawContext c, List<Row> rows, int mx, int my, boolean withProgress) {
        int lx1 = X(235), lx2 = X(735), ly1 = Y(90), ly2 = Y(610), pitch = Y(105), rh = Y(97);
        float max = Math.max(0, rows.size() * pitch - (ly2 - ly1));
        scroll = MathHelper.clamp(scroll, 0, max);

        c.fill(lx1, Y(40), lx2, Y(82), 0xFF0A1018);   // caja del buscador

        c.enableScissor(lx1, ly1, X(750), ly2);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = ly1 + i * pitch - (int) scroll;
            if (y + rh < ly1 || y > ly2) continue;
            boolean sel = r.id.equals(selectedId);
            boolean hov = in(mx, my, lx1, Math.max(y, ly1), lx2, Math.min(y + rh, ly2));
            c.fill(lx1, y, lx2, y + rh, sel || hov ? 0xFF4A4F55 : 0xFF2F3439);

            // punto de estado + minicono de tipo
            int dot = Math.max(3, X(10));
            int dotColor = r.state == State.CLAIMED ? 0xFFE0B020 : r.locked ? 0xFF3A3A3A : 0xFF2FCB45;
            c.fill(X(245), y + Y(16), X(245) + dot, y + Y(16) + dot, dotColor);

            // caja del ícono
            int ix1 = X(272), iy1 = y + Y(10), ix2 = X(344), iy2 = y + Y(82);
            c.fill(ix1, iy1, ix2, iy2, 0xFF1A1E24);
            int ipx = Math.min(ix2 - ix1, iy2 - iy1) * 4 / 5;
            Ui.item(c, r.icon, (ix1 + ix2) / 2 - ipx / 2, (iy1 + iy2) / 2 - ipx / 2, ipx);
            if (r.locked) c.fill(ix1, iy1, ix2, iy2, 0xB0000000);

            // título (franja oscura + texto cian)
            int tx = X(356), tw = X(725) - tx;
            c.fill(X(355), y + Y(13), X(725), y + Y(13) + fh() + 2, 0xFF05080C);
            if (r.secret) Ui.rainbowText(c, textRenderer, r.title, tx, y + Y(14));
            else c.drawText(textRenderer, r.title, tx, y + Y(14), r.locked ? 0xFF2A9D8F : Ui.CYAN, true);

            // descripción recortada con "…"
            if (!r.sub.isEmpty()) {
                int base = r.locked ? 0x888888 : 0xFFFFFF, hi = r.locked ? 0x3A3A9A : Ui.BLUE_HI;
                c.drawText(textRenderer, ellipsis(RichText.parse(r.sub, base, hi), tw, base), tx, y + Y(14) + fh() + 5, 0xFF000000 | base, true);
            }

            // barra de progreso + contador
            if (withProgress) {
                int bx1 = X(352), bx2 = X(650), by1 = y + Y(74), by2 = by1 + Math.max(3, Y(9));
                c.fill(bx1, by1, bx2, by2, Ui.BAR_BACK);
                if (!r.locked && r.goal > 0) c.fill(bx1, by1, bx1 + (bx2 - bx1) * Math.min(r.progress, r.goal) / r.goal, by2, Ui.BAR_FILL);
                String cnt = r.progress + "/" + r.goal;
                c.drawText(textRenderer, cnt, X(700) - textRenderer.getWidth(cnt), by1 - 1, r.locked ? 0xFF777777 : Ui.WHITE, true);
            }
        }
        c.disableScissor();

        // barra de scroll
        int sx1 = X(742), sx2 = X(748);
        c.fill(sx1, ly1, sx2, ly2, 0x22FFFFFF);
        float total = Math.max(rows.size() * pitch, 1);
        int thumbH = (int) Math.min(ly2 - ly1, (ly2 - ly1) * (ly2 - ly1) / total);
        int thumbY = max <= 0 ? ly1 : ly1 + (int) ((ly2 - ly1 - thumbH) * (scroll / max));
        c.fill(sx1, thumbY, sx2, thumbY + thumbH, 0xFFFFFFFF);
    }

    private OrderedText ellipsis(Text t, int w, int base) {
        if (textRenderer.getWidth(t) <= w) return t.asOrderedText();
        int dots = textRenderer.getWidth("…");
        return OrderedText.concat(Language.getInstance().reorder(textRenderer.trimToWidth(t, w - dots)),
                OrderedText.styledForwardsVisitedString("…", Style.EMPTY.withColor(base)));
    }

    private void scaled(DrawContext c, String s, int x, int y, int color, float sc) {
        var m = c.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(sc, sc, 1f);
        c.drawText(textRenderer, s, 0, 0, color, true);
        m.pop();
    }

    private int wrap(DrawContext c, Text t, int x, int y, int w) {
        for (OrderedText l : textRenderer.wrapLines(t, w)) {
            c.drawText(textRenderer, l, x, y, Ui.WHITE, true);
            y += fh() + 2;
        }
        return y;
    }

    private void slot(DrawContext c, ItemStack s, int x, int y, int size, int mx, int my) {
        c.fill(x, y, x + size, y + size, Ui.BG_SLOT);
        Ui.border(c, x, y, x + size, y + size, Ui.BORDER_SLOT);
        Ui.item(c, s, x + size / 8, y + size / 8, size * 3 / 4);
        if (s.getCount() > 1) {
            String n = "x" + s.getCount();
            c.drawText(textRenderer, n, x + size - textRenderer.getWidth(n) - 2, y + size - fh(), Ui.WHITE, true);
        }
        if (in(mx, my, x, y, x + size, y + size)) tipStack = s;
    }

    // ---- detalle de misión (panel derecho)
    private void drawQuestDetail(DrawContext c, int mx, int my) {
        Content.Quest q = quest(selectedId);
        if (q == null) return;
        State st = QuestLogic.state(ClientData.me, q);
        int x0 = X(775), w = X(1250) - x0;
        scaled(c, q.title, x0, Y(48), Ui.CYAN, 1.6f);

        int ip = Math.min(X(90), Y(90));
        Ui.item(c, ItemSpec.parse(q.icon), x0 + w / 2 - ip / 2, Y(112), ip);
        int y = Y(112) + ip + Y(12);
        y = wrap(c, RichText.parse(q.description, 0xFFFFFF, Ui.BLUE_HI), x0, y, w) + 3;

        if (st == State.LOCKED) {
            c.drawText(textRenderer, "Bloqueada. Requiere:", x0, y, Ui.RED, true); y += fh() + 2;
            for (String r : q.requires) {
                if (ClientData.me.claimed.contains(r)) continue;
                Content.Quest rq = quest(r);
                c.drawText(textRenderer, "  - " + (rq != null ? rq.title : r), x0, y, Ui.RED, true); y += fh() + 2;
            }
            y += 3;
        }
        if (!q.objectives.isEmpty()) {
            c.drawText(textRenderer, "Objetivos:", x0, y, Ui.GOLD, true); y += fh() + 2;
            for (String o : q.objectives) { c.drawText(textRenderer, "  - " + o, x0, y, Ui.WHITE, true); y += fh() + 2; }
            y += 3;
        }
        if (!q.rewards.isEmpty()) {
            c.drawText(textRenderer, "Recompensas:", x0, y, Ui.GOLD, true); y += fh() + 3;
            int sl = Math.min(X(66), Y(62)), sx = x0;
            for (String r : q.rewards) { slot(c, ItemSpec.parse(r), sx, y, sl, mx, my); sx += sl + X(12); }
        }
        // barra inferior de progreso
        int prog = ClientData.me.progress.getOrDefault(q.id, 0);
        int bx1 = X(780), bx2 = X(1220), by1 = Y(548), by2 = by1 + Math.max(3, Y(10));
        c.fill(bx1, by1, bx2, by2, 0xFF1A2230);
        if (q.goal > 0 && st != State.LOCKED) c.fill(bx1, by1, bx1 + (bx2 - bx1) * Math.min(prog, q.goal) / q.goal, by2, Ui.BAR_FILL);
        String cnt = prog + "/" + q.goal;
        c.drawText(textRenderer, cnt, (bx1 + bx2) / 2 - textRenderer.getWidth(cnt) / 2, Y(566), Ui.WHITE, true);
    }

    // ---- detalle de Guía / Historia
    private void drawTextDetail(DrawContext c, int mx, int my) {
        String title = null, text = null, icon = null, locked = null;
        boolean secret = false;
        if (tab == Tab.GUIDE) {
            for (Content.Guide g : ClientData.content.guide) if (g.id.equals(selectedId)) { title = g.title; text = g.text; icon = g.icon; }
        } else {
            for (Content.Lore l : ClientData.content.lore) if (l.id.equals(selectedId)) {
                icon = l.icon;
                if (QuestLogic.loreUnlocked(ClientData.me, l)) { title = l.title; text = l.text; }
                else {
                    secret = true; title = "?????"; text = "";
                    for (Content.Quest q : ClientData.content.quests) if (q.id.equals(l.unlockedBy)) locked = "Completa «" + q.title + "» para desbloquear.";
                }
            }
        }
        if (title == null) return;
        int x0 = X(775), w = X(1250) - x0;
        if (secret) Ui.rainbowText(c, textRenderer, title, x0, Y(52));
        else scaled(c, title, x0, Y(48), Ui.CYAN, 1.6f);
        int ip = Math.min(X(90), Y(90));
        Ui.item(c, ItemSpec.parse(icon), x0 + w / 2 - ip / 2, Y(112), ip);
        if (secret) c.fill(x0 + w / 2 - ip / 2, Y(112), x0 + w / 2 + ip / 2, Y(112) + ip, 0xC0000000);
        int y = Y(112) + ip + Y(14);
        if (locked != null) c.drawText(textRenderer, locked, x0, y, Ui.RED, true);
        else wrap(c, RichText.parse(text, 0xFFFFFF, Ui.BLUE_HI), x0, y, w);
    }

    // ---- bestiario: cuadrícula
    private int[] mobCell(int i) {
        int cols = 3, gap = X(10);
        int x1 = X(262), x2 = X(1230);
        int cw = (x2 - x1 - gap * (cols - 1)) / cols, ch = Y(80);
        return new int[]{x1 + (i % cols) * (cw + gap), Y(100) + (i / cols) * (ch + gap) - (int) scroll, cw, ch};
    }

    private List<Content.Mob> filteredMobs() {
        String f = searchText.toLowerCase(Locale.ROOT);
        List<Content.Mob> out = new ArrayList<>();
        for (Content.Mob m : ClientData.content.bestiary) {
            boolean d = QuestLogic.discovered(ClientData.me, m);
            if (!d || m.name.toLowerCase(Locale.ROOT).contains(f)) out.add(m);
        }
        return out;
    }

    private ItemStack mobIcon(Content.Mob m) {
        Identifier id = Identifier.tryParse(m.entity.isBlank() ? (m.id.contains(":") ? m.id : "minecraft:" + m.id) : m.entity);
        if (id != null) {
            Optional<EntityType<?>> t = Registries.ENTITY_TYPE.getOrEmpty(id);
            if (t.isPresent()) {
                SpawnEggItem egg = SpawnEggItem.forEntity(t.get());
                if (egg != null) return new ItemStack(egg);
            }
        }
        return new ItemStack(Items.BARRIER);
    }

    private void drawMobGrid(DrawContext c, int mx, int my) {
        List<Content.Mob> mobs = filteredMobs();
        c.fill(X(262), Y(40), X(1230), Y(82), 0xFF0A1018);
        c.enableScissor(X(262), Y(96), X(1235), Y(615));
        for (int i = 0; i < mobs.size(); i++) {
            Content.Mob m = mobs.get(i);
            int[] r = mobCell(i);
            boolean disc = QuestLogic.discovered(ClientData.me, m);
            boolean hov = in(mx, my, r[0], r[1], r[0] + r[2], r[1] + r[3]) && my > Y(96);
            c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hov && disc ? 0xFF4A4F55 : 0xFF2F3439);
            int is = r[3] - Y(16);
            c.fill(r[0] + Y(8), r[1] + Y(8), r[0] + Y(8) + is, r[1] + Y(8) + is, 0xFF1A1E24);
            Ui.item(c, mobIcon(m), r[0] + Y(8) + is / 10, r[1] + Y(8) + is / 10, is * 4 / 5);
            if (!disc) c.fill(r[0] + Y(8), r[1] + Y(8), r[0] + Y(8) + is, r[1] + Y(8) + is, 0xC8000000);
            int tx = r[0] + is + Y(20);
            if (disc) {
                c.drawText(textRenderer, m.name, tx, r[1] + Y(12), Ui.CYAN, true);
                drawDanger(c, tx, r[1] + Y(12) + fh() + 6, m.danger, Math.max(4, X(14)));
            } else {
                Ui.rainbowText(c, textRenderer, "???", tx, r[1] + Y(12));
            }
        }
        c.disableScissor();
    }

    private void drawDanger(DrawContext c, int x, int y, int danger, int sq) {
        for (int i = 0; i < 5; i++) c.fill(x + i * (sq + 3), y, x + i * (sq + 3) + sq, y + sq, i < danger ? 0xFFE03030 : 0xFF1B2435);
    }

    // ---- bestiario: ficha
    private LivingEntity entity(Content.Mob m) {
        if (entities.containsKey(m.id)) return entities.get(m.id);
        LivingEntity result = null;
        Identifier id = Identifier.tryParse(m.entity.isBlank() ? (m.id.contains(":") ? m.id : "minecraft:" + m.id) : m.entity);
        if (id != null && client != null && client.world != null) {
            Optional<EntityType<?>> t = Registries.ENTITY_TYPE.getOrEmpty(id);
            if (t.isPresent()) {
                Entity e = t.get().create(client.world);
                if (e instanceof LivingEntity le) result = le;
            }
        }
        entities.put(m.id, result);
        return result;
    }

    private void drawMobDetail(DrawContext c, int mx, int my) {
        Content.Mob m = null;
        for (Content.Mob x : ClientData.content.bestiary) if (x.id.equals(selectedId)) m = x;
        if (m == null) { mobDetail = false; return; }

        scaled(c, m.name, X(262), Y(48), Ui.CYAN, 1.6f);
        int px1 = X(262), py1 = Y(107), px2 = X(527), py2 = Y(507);
        c.fill(px1, py1, px2, py2, 0xFF0D1119);
        Ui.border(c, px1, py1, px2, py2, Ui.BORDER_SLOT);
        LivingEntity e = entity(m);
        if (e != null) {
            int size = (int) (0.75f * (py2 - py1) / Math.max(e.getHeight(), 0.6f));
            InventoryScreen.drawEntity(c, px1, py1, px2, py2, size, 0.0625f, mx, my, e);
        } else {
            int ip = Math.min(px2 - px1, py2 - py1) / 2;
            Ui.item(c, mobIcon(m), (px1 + px2) / 2 - ip / 2, (py1 + py2) / 2 - ip / 2, ip);
        }

        int tx = X(560), tw = X(1235) - tx, y = Y(112);
        c.drawText(textRenderer, "Peligro:", tx, y, Ui.GOLD, true);
        drawDanger(c, tx + textRenderer.getWidth("Peligro:") + X(14), y - 1, m.danger, Math.max(5, X(18)));
        y = Y(150);
        c.drawText(textRenderer, "Descripción", tx, y, Ui.GOLD, true); y += fh() + 3;
        y = wrap(c, RichText.parse(m.description, 0xFFFFFF, Ui.BLUE_HI), tx, y, tw) + Y(10);
        c.drawText(textRenderer, "Qué hacer", tx, y, Ui.GOLD, true); y += fh() + 3;
        wrap(c, RichText.parse(m.tip, 0xFFFFFF, Ui.BLUE_HI), tx, y, tw);

        c.drawText(textRenderer, "Botín:", tx, Y(450), Ui.GOLD, true);
        int sl = Math.min(X(66), Y(60)), sx = tx;
        for (String l : m.loot) { slot(c, ItemSpec.parse(l), sx, Y(473), sl, mx, my); sx += sl + X(10); }
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        for (int i = 0; i < 4; i++) {
            int[] s = SLOT[i];
            if (in(mx, my, X(s[0]), Y(s[1]), X(s[2]), Y(s[3]))) { if (Tab.values()[i] != tab) switchTab(Tab.values()[i]); return true; }
        }
        if (tab == Tab.BESTIARY) {
            if (!mobDetail && my > Y(96)) {
                List<Content.Mob> mobs = filteredMobs();
                for (int i = 0; i < mobs.size(); i++) {
                    int[] r = mobCell(i);
                    if (in(mx, my, r[0], r[1], r[0] + r[2], r[1] + r[3]) && QuestLogic.discovered(ClientData.me, mobs.get(i))) {
                        selectedId = mobs.get(i).id; mobDetail = true; clearAndInit(); return true;
                    }
                }
            }
            return false;
        }
        if (in(mx, my, X(235), Y(90), X(742), Y(610))) {
            List<Row> rs = rows();
            int idx = (int) ((my - Y(90) + scroll) / Y(105));
            if (idx >= 0 && idx < rs.size()) { selectedId = rs.get(idx).id; clearAndInit(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= (float) (v * Y(40));
        if (scroll < 0) scroll = 0;
        if (tab == Tab.BESTIARY && !mobDetail) {
            int rows = (filteredMobs().size() + 2) / 3;
            float max = Math.max(0, rows * (Y(80) + X(10)) - (Y(615) - Y(100)));
            scroll = Math.min(scroll, max);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (search != null && search.isFocused()) return false;   // no cerrar al escribir una "g"
        if (DesafioMenuClient.OPEN_KEY.matchesKey(keyCode, scanCode)) { close(); return true; }   // G cierra el menú
        return false;
    }
}
