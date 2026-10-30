package com.desafio.menu.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Carga/guarda el contenido en config/desafio_menu/*.json (lado servidor). */
public final class ContentStore {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger LOG = LoggerFactory.getLogger("desafio_menu");
    private static Content content = new Content();

    private ContentStore() {}

    public static Content get() { return content; }

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("desafio_menu"); }

    public static void load() {
        try { Files.createDirectories(dir()); } catch (IOException e) { LOG.error("No se pudo crear {}", dir(), e); }
        Content c = new Content();
        c.quests = read("quests.json", new TypeToken<List<Content.Quest>>() {}.getType(), Defaults::quests);
        c.guide = read("guide.json", new TypeToken<List<Content.Guide>>() {}.getType(), Defaults::guide);
        c.lore = read("lore.json", new TypeToken<List<Content.Lore>>() {}.getType(), Defaults::lore);
        c.bestiary = read("bestiary.json", new TypeToken<List<Content.Mob>>() {}.getType(), Defaults::bestiary);
        c.recipes = read("recipes.json", Content.Recipes.class, Content.Recipes::new);
        content = c;
    }

    private static <T> T read(String file, Type type, Supplier<T> def) {
        Path p = dir().resolve(file);
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                T v = GSON.fromJson(r, type);
                if (v != null) return v;
            } catch (Exception e) { LOG.error("JSON inválido en {} (se usan valores por defecto)", p, e); }
        }
        T v = def.get();
        write(file, v);
        return v;
    }

    private static void write(String file, Object o) {
        try (Writer w = Files.newBufferedWriter(dir().resolve(file))) { GSON.toJson(o, w); }
        catch (IOException e) { LOG.error("No se pudo escribir {}", file, e); }
    }

    /** Aplica una edición proveniente del cliente. Devuelve true si cambió algo. */
    public static boolean apply(String category, String op, String json) {
        try {
            switch (category) {
                case "quests" -> { upsertOrDelete(content.quests, op, json, Content.Quest.class, q -> q.id); write("quests.json", content.quests); }
                case "guide" -> { upsertOrDelete(content.guide, op, json, Content.Guide.class, g -> g.id); write("guide.json", content.guide); }
                case "lore" -> { upsertOrDelete(content.lore, op, json, Content.Lore.class, l -> l.id); write("lore.json", content.lore); }
                case "bestiary" -> { upsertOrDelete(content.bestiary, op, json, Content.Mob.class, m -> m.id); write("bestiary.json", content.bestiary); }
                case "recipes" -> {
                    switch (op) {
                        case "remove_recipe" -> { if (!content.recipes.removed.contains(json)) content.recipes.removed.add(json); }
                        case "restore_recipe" -> content.recipes.removed.remove(json);
                        default -> upsertOrDelete(content.recipes.custom, op, json, Content.Custom.class, c -> c.id);
                    }
                    write("recipes.json", content.recipes);
                }
                default -> { return false; }
            }
            return true;
        } catch (Exception e) {
            LOG.error("Edición inválida ({} {})", category, op, e);
            return false;
        }
    }

    private static <T> void upsertOrDelete(List<T> list, String op, String json, Class<T> cls, Function<T, String> idOf) {
        if (op.equals("delete")) {
            list.removeIf(x -> idOf.apply(x).equals(json));
            return;
        }
        T item = GSON.fromJson(json, cls);
        String id = idOf.apply(item);
        if (id == null || id.isBlank()) throw new JsonParseException("id vacío");
        for (int i = 0; i < list.size(); i++) {
            if (idOf.apply(list.get(i)).equals(id)) { list.set(i, item); return; }
        }
        list.add(item);
    }
}
