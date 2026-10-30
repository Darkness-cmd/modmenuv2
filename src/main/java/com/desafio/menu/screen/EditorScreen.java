package com.desafio.menu.screen;

import com.desafio.menu.client.ClientData;
import com.desafio.menu.data.Content;
import com.desafio.menu.data.ContentStore;
import com.desafio.menu.networking.EditPayload;
import com.desafio.menu.screen.EntryFormScreen.Field;
import com.desafio.menu.screen.EntryFormScreen.Type;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/** Panel de administración: edita misiones, guía, historia, bestiario y recetas. Todo se guarda en config/desafio_menu/*.json (servidor). */
public class EditorScreen extends Screen implements ClientData.Refreshable {
    private enum Cat {
        QUESTS("quests", "Misiones", "+ Nueva misión"),
        GUIDE("guide", "Guía", "+ Nueva entrada"),
        LORE("lore", "Historia", "+ Nueva entrada"),
        BESTIARY("bestiary", "Bestiario", "+ Nuevo mob"),
        RECIPES("recipes", "Recetas", "+ Nueva receta");
        final String id, label, newLabel;
        Cat(String id, String label, String newLabel) { this.id = id; this.label = label; this.newLabel = newLabel; }
    }
    private enum Sub { GAME("Del juego"), CUSTOM("Personalizadas"), REMOVED("Quitadas"); final String label; Sub(String l) { label = l; } }

    private record ERow(String id, String title, String sub, ItemStack icon, Object ref) {}

    private final Screen parent;
    private Cat cat = Cat.QUESTS;
    private Sub sub = Sub.GAME;
    private float scroll = 0;
    private String searchText = "";
    private List<ERow> rows = new ArrayList<>();

    public EditorScreen(Screen parent) { super(Text.literal("Editor")); this.parent = parent; }

    private int X(float v) { return Math.round(v * width / 1280f); }
    private int Y(float v) { return Math.round(v * height / 674f); }
    private static boolean in(double mx, double my, int x1, int y1, int x2, int y2) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }

    @Override public void refresh() { clearAndInit(); }
    @Override public void close() { client.setScreen(parent); }

    // ------------------------------------------------------------ init
    @Override
    protected void init() {
        rebuildRows();
        Cat[] cats = Cat.values();
        for (int i = 0; i < cats.length; i++) {
            Cat k = cats[i];
            ButtonWidget b = ButtonWidget.builder(Text.literal(k.label), x -> { cat = k; scroll = 0; searchText = ""; clearAndInit(); })
                    .dimensions(X(28 + i * 235), Y(22), X(225), Y(40)).build();
            b.active = k != cat;
            addDrawableChild(b);
        }
        if (cat == Cat.RECIPES) {
            Sub[] subs = Sub.values();
            int[][] pos = {{28, 270}, {280, 520}, {530, 770}};
            for (int i = 0; i < 3; i++) {
                Sub s = subs[i];
                ButtonWidget b = ButtonWidget.builder(Text.literal(s.label), x -> { sub = s; scroll = 0; clearAndInit(); })
                        .dimensions(X(pos[i][0]), Y(76), X(pos[i][1] - pos[i][0]), Y(40)).build();
                b.active = s != sub;
                addDrawableChild(b);
            }
            TextFieldWidget sf = new TextFieldWidget(textRenderer, X(790), Y(80), X(1252) - X(790), Y(32), Text.empty());
            sf.setPlaceholder(Text.literal("Buscar receta..."));
            sf.setText(searchText);
            sf.setChangedListener(s -> { searchText = s; scroll = 0; rebuildRows(); });
            addDrawableChild(sf);
            if (sub == Sub.CUSTOM) addNewButton();
        } else {
            addNewButton();
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), b -> close())
                .dimensions(X(536), Y(619), X(744) - X(536), Y(38)).build());
    }

    private void addNewButton() {
        addDrawableChild(ButtonWidget.builder(Text.literal(cat.newLabel), b ->
                client.setScreen(new EntryFormScreen(this, cat.id, new JsonObject(), fields(cat), true)))
                .dimensions(X(28), Y(76), X(318) - X(28), Y(40)).build());
    }

    static List<Field> fields(Cat c) {
        return switch (c) {
            case QUESTS -> List.of(new Field("id", "id", Type.STR), new Field("title", "Título", Type.STR), new Field("description", "Descripción ([[azul]])", Type.STR),
                    new Field("icon", "Ícono (item id)", Type.STR), new Field("goal", "Meta (número)", Type.INT), new Field("type", "Tipo: kill|obtain|visit|custom", Type.STR),
                    new Field("target", "Objetivo (entity/item/dimensión)", Type.STR), new Field("requires", "Requiere (ids)", Type.LIST),
                    new Field("objectives", "Objetivos (texto)", Type.LIST), new Field("rewards", "Recompensas (item*cant)", Type.LIST));
            case GUIDE -> List.of(new Field("id", "id", Type.STR), new Field("title", "Título", Type.STR), new Field("text", "Texto", Type.STR), new Field("icon", "Ícono (item id)", Type.STR));
            case LORE -> List.of(new Field("id", "id", Type.STR), new Field("title", "Título", Type.STR), new Field("text", "Texto", Type.STR),
                    new Field("icon", "Ícono (item id)", Type.STR), new Field("unlockedBy", "Se desbloquea con (id misión)", Type.STR));
            case BESTIARY -> List.of(new Field("id", "id", Type.STR), new Field("name", "Nombre", Type.STR), new Field("danger", "Peligro (1-5)", Type.INT),
                    new Field("description", "Descripción", Type.STR), new Field("tip", "Qué hacer", Type.STR), new Field("loot", "Botín (items)", Type.LIST),
                    new Field("discover", "Se descubre: kill|always", Type.STR), new Field("entity", "Entidad (opcional)", Type.STR));
            case RECIPES -> List.of(new Field("id", "id de receta (ns:nombre)", Type.STR), new Field("recipe", "Receta (JSON vanilla)", Type.RAW));
        };
    }

    // ------------------------------------------------------------ filas
    private void rebuildRows() {
        List<ERow> out = new ArrayList<>();
        Content c = ClientData.content;
        String f = searchText.toLowerCase(Locale.ROOT);
        switch (cat) {
            case QUESTS -> { for (Content.Quest q : c.quests) out.add(new ERow(q.id, q.title, q.id + "   meta: " + q.goal + "   tipo: " + q.type, stack(q.icon), q)); }
            case GUIDE -> { for (Content.Guide g : c.guide) out.add(new ERow(g.id, g.title, g.id, stack(g.icon), g)); }
            case LORE -> { for (Content.Lore l : c.lore) out.add(new ERow(l.id, l.title, l.id + (l.unlockedBy.isBlank() ? "" : "   requiere: " + l.unlockedBy), stack(l.icon), l)); }
            case BESTIARY -> { for (Content.Mob m : c.bestiary) out.add(new ERow(m.id, m.name, m.id + "   peligro: " + m.danger + "   se descubre: " + m.discover, egg(m), m)); }
            case RECIPES -> {
                Set<String> removed = new HashSet<>(c.recipes.removed);
                if (sub == Sub.GAME && client != null && client.world != null) {
                    var reg = client.world.getRegistryManager();
                    for (RecipeEntry<?> e : client.world.getRecipeManager().values()) {
                        String id = e.id().toString();
                        if (removed.contains(id)) continue;
                        ItemStack res = e.value().getResult(reg);
                        String name = res.isEmpty() ? id : res.getName().getString();
                        if (!f.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(f) && !id.contains(f)) continue;
                        out.add(new ERow(id, name, id, res, null));
                    }
                    out.sort(Comparator.comparing(ERow::title));
                } else if (sub == Sub.CUSTOM) {
                    for (Content.Custom cu : c.recipes.custom) if (f.isEmpty() || cu.id.contains(f)) out.add(new ERow(cu.id, cu.id, "personalizada", new ItemStack(Items.CRAFTING_TABLE), cu));
                } else if (sub == Sub.REMOVED) {
                    for (String id : c.recipes.removed) {
                        if (!f.isEmpty() && !id.contains(f)) continue;
                        Identifier ident = Identifier.tryParse(id);
                        ItemStack s = ident == null ? new ItemStack(Items.BARRIER) : new ItemStack(Registries.ITEM.get(ident));
                        out.add(new ERow(id, id, "quitada", s.isEmpty() ? new ItemStack(Items.BARRIER) : s, null));
                    }
                }
            }
        }
        rows = out;
    }

    private static ItemStack stack(String spec) { return com.desafio.menu.data.ItemSpec.parse(spec); }

    private static ItemStack egg(Content.Mob m) {
        Identifier id = Identifier.tryParse(m.entity.isBlank() ? (m.id.contains(":") ? m.id : "minecraft:" + m.id) : m.entity);
        if (id != null) {
            Optional<EntityType<?>> t = Registries.ENTITY_TYPE.getOrEmpty(id);
            if (t.isPresent()) { SpawnEggItem e = SpawnEggItem.forEntity(t.get()); if (e != null) return new ItemStack(e); }
        }
        return new ItemStack(Items.STONE);
    }

    // ------------------------------------------------------------ geometría
    private int listTop() { return Y(130); }
    private int listBottom() { return Y(612); }
    private int pitch() { return Y(59); }
    private int rowH() { return Y(52); }
    private boolean twoButtons() { return cat != Cat.RECIPES || sub == Sub.CUSTOM; }

    private int[] btn(int slot, int rowY) {      // slot 0 = Editar, 1 = Borrar/Quitar/Restaurar
        int bh = Y(36), by = rowY + (rowH() - bh) / 2;
        return slot == 0 ? new int[]{X(902), by, X(1066), by + bh} : new int[]{X(1078), by, X(1243), by + bh};
    }

    // ------------------------------------------------------------ render
    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0xFF070D18);
        Ui.border(c, X(10), Y(8), X(1270), Y(666), 0xFF1D2A40);

        int top = listTop(), bot = listBottom();
        float max = Math.max(0, rows.size() * pitch() - (bot - top));
        scroll = MathHelper.clamp(scroll, 0, max);

        c.enableScissor(X(28), top, X(1252), bot);
        for (int i = 0; i < rows.size(); i++) {
            ERow r = rows.get(i);
            int y = top + i * pitch() - (int) scroll;
            if (y + rowH() < top || y > bot) continue;
            c.fill(X(28), y, X(1252), y + rowH(), 0xFF0F1B2E);
            Ui.item(c, r.icon, X(40), y + (rowH() - Y(36)) / 2, Y(36));
            int maxW = X(890) - X(95);
            c.drawText(textRenderer, textRenderer.trimToWidth(r.title, maxW), X(95), y + Y(7), Ui.CYAN, true);
            c.drawText(textRenderer, textRenderer.trimToWidth(r.sub, maxW), X(95), y + Y(7) + textRenderer.fontHeight + 2, Ui.GRAY, true);

            if (twoButtons()) drawBtn(c, btn(0, y), "Editar", 0xFF2B4A7C, 0xFF4A6FA5, mx, my);
            String second = twoButtons() ? "Borrar" : (sub == Sub.GAME ? "Quitar" : "Restaurar");
            boolean red = twoButtons();
            drawBtn(c, btn(1, y), second, red ? 0xFF6B1F1F : 0xFF2B4A7C, red ? 0xFFA03A3A : 0xFF4A6FA5, mx, my);
        }
        c.disableScissor();
    }

    private void drawBtn(DrawContext c, int[] r, String label, int bg, int border, int mx, int my) {
        boolean hov = in(mx, my, r[0], r[1], r[2], r[3]);
        c.fill(r[0], r[1], r[2], r[3], hov ? border : bg);
        Ui.border(c, r[0], r[1], r[2], r[3], border);
        c.drawCenteredTextWithShadow(textRenderer, label, (r[0] + r[2]) / 2, (r[1] + r[3]) / 2 - textRenderer.fontHeight / 2, Ui.WHITE);
    }

    // ------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || my < listTop() || my > listBottom()) return false;
        int idx = (int) ((my - listTop() + scroll) / pitch());
        if (idx < 0 || idx >= rows.size()) return false;
        ERow r = rows.get(idx);
        int y = listTop() + idx * pitch() - (int) scroll;
        if (my > y + rowH()) return false;

        if (twoButtons() && hit(btn(0, y), mx, my)) {
            JsonObject o = ContentStore.GSON.toJsonTree(r.ref).getAsJsonObject();
            client.setScreen(new EntryFormScreen(this, cat.id, o, fields(cat), false));
            return true;
        }
        if (hit(btn(1, y), mx, my)) {
            if (twoButtons()) send(cat.id, "delete", r.id);
            else send("recipes", sub == Sub.GAME ? "remove_recipe" : "restore_recipe", r.id);
            return true;
        }
        return false;
    }

    private static boolean hit(int[] r, double mx, double my) { return in(mx, my, r[0], r[1], r[2], r[3]); }

    private void send(String category, String op, String json) { ClientPlayNetworking.send(new EditPayload(category, op, json)); }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= (float) (v * Y(40));
        return true;
    }
}
