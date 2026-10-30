package com.desafio.menu.screen;

import com.desafio.menu.networking.EditPayload;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Formulario genérico de edición de una entrada (misión, guía, lore, mob o receta personalizada). */
public class EntryFormScreen extends Screen {
    public enum Type { STR, INT, LIST, RAW }
    public record Field(String key, String label, Type type) {}

    private final Screen parent;
    private final String category;
    private final JsonObject initial;
    private final List<Field> fields;
    private final boolean isNew;
    private final List<TextFieldWidget> widgets = new ArrayList<>();
    private String error = "";

    public EntryFormScreen(Screen parent, String category, JsonObject initial, List<Field> fields, boolean isNew) {
        super(Text.literal(isNew ? "Nueva entrada" : "Editar entrada"));
        this.parent = parent; this.category = category; this.initial = initial; this.fields = fields; this.isNew = isNew;
    }

    @Override
    protected void init() {
        widgets.clear();
        int colW = (width - 60) / 2;
        int perCol = (fields.size() + 1) / 2;
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            int col = i / perCol, row = i % perCol;
            int x = 20 + col * (colW + 20), y = 44 + row * 36;
            TextFieldWidget w = new TextFieldWidget(textRenderer, x, y + 11, colW, 18, Text.literal(f.label));
            w.setMaxLength(f.type == Type.RAW ? 4000 : 600);
            w.setText(read(f));
            if (f.key.equals("id") && !isNew) w.setEditable(false);   // el id identifica la entrada
            widgets.add(w);
            addDrawableChild(w);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Guardar"), b -> save()).dimensions(width / 2 - 105, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"), b -> close()).dimensions(width / 2 + 5, height - 28, 100, 20).build());
    }

    private String read(Field f) {
        JsonElement e = initial.get(f.key);
        if (e == null || e.isJsonNull()) return "";
        switch (f.type) {
            case LIST -> {
                List<String> parts = new ArrayList<>();
                if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) parts.add(x.getAsString());
                return String.join(" | ", parts);
            }
            case RAW -> { return e.toString(); }
            default -> { return e.getAsString(); }
        }
    }

    private void save() {
        JsonObject o = new JsonObject();
        try {
            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i);
                String v = widgets.get(i).getText().trim();
                if (f.key.equals("type") || f.key.equals("discover")) v = v.toLowerCase(java.util.Locale.ROOT);
                switch (f.type) {
                    case STR -> o.addProperty(f.key, v);
                    case INT -> o.addProperty(f.key, v.isEmpty() ? 0 : Integer.parseInt(v));
                    case LIST -> {
                        JsonArray a = new JsonArray();
                        for (String p : v.split("\\|")) if (!p.trim().isEmpty()) a.add(p.trim());
                        o.add(f.key, a);
                    }
                    case RAW -> o.add(f.key, JsonParser.parseString(v.isEmpty() ? "{}" : v));
                }
            }
            if (!o.has("id") || o.get("id").getAsString().isBlank()) { error = "El campo id es obligatorio."; return; }
            if (o.has("type") && !List.of("kill", "obtain", "visit", "custom").contains(o.get("type").getAsString())) {
                error = "Tipo inválido: usa kill, obtain, visit o custom."; return;
            }
            if (o.has("discover") && !List.of("kill", "always").contains(o.get("discover").getAsString())) {
                error = "Se descubre: usa kill o always."; return;
            }
        } catch (NumberFormatException e) { error = "Un campo numérico no es válido."; return; }
        catch (JsonParseException e) { error = "JSON inválido en un campo."; return; }

        ClientPlayNetworking.send(new EditPayload(category, "upsert", o.toString()));
        close();
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0xFF050A12);
        c.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFF55FFFF);
        int colW = (width - 60) / 2, perCol = (fields.size() + 1) / 2;
        for (int i = 0; i < fields.size(); i++) {
            int col = i / perCol, row = i % perCol;
            String hint = switch (fields.get(i).type) { case LIST -> " (separar con |)"; case RAW -> " (JSON)"; default -> ""; };
            c.drawText(textRenderer, fields.get(i).label + hint, 20 + col * (colW + 20), 44 + row * 36, 0xFFFFAA00, true);
        }
        if (!error.isEmpty()) c.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 42, 0xFFFF5555);
    }

    @Override public void close() { client.setScreen(parent); }
}
