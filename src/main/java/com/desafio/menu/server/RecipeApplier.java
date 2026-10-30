package com.desafio.menu.server;

import com.desafio.menu.data.ContentStore;
import com.desafio.menu.data.Content;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** Quita recetas y añade recetas personalizadas (JSON de receta vanilla) sobre el RecipeManager en caliente. */
public final class RecipeApplier {
    private static final Logger LOG = LoggerFactory.getLogger("desafio_menu");
    private RecipeApplier() {}

    public static void apply(MinecraftServer server) {
        try {
            RecipeManager rm = server.getRecipeManager();
            Content.Recipes r = ContentStore.get().recipes;

            Set<Identifier> removed = new HashSet<>();
            for (String s : r.removed) { Identifier id = Identifier.tryParse(s); if (id != null) removed.add(id); }
            Set<Identifier> customIds = new HashSet<>();
            for (Content.Custom c : r.custom) { Identifier id = Identifier.tryParse(c.id); if (id != null) customIds.add(id); }

            List<RecipeEntry<?>> out = new ArrayList<>();
            for (RecipeEntry<?> e : rm.values()) {
                if (!removed.contains(e.id()) && !customIds.contains(e.id())) out.add(e);
            }
            RegistryOps<JsonElement> ops = server.getRegistryManager().getOps(JsonOps.INSTANCE);
            for (Content.Custom c : r.custom) {
                Identifier id = Identifier.tryParse(c.id);
                if (id == null || removed.contains(id)) continue;
                try {
                    Recipe<?> rec = Recipe.CODEC.parse(ops, c.recipe).getOrThrow(IllegalStateException::new);
                    out.add(new RecipeEntry<>(id, rec));
                } catch (Exception ex) { LOG.warn("Receta personalizada inválida {}: {}", c.id, ex.getMessage()); }
            }
            rm.setRecipes(out);
            server.getPlayerManager().onDataPacksReloaded();
        } catch (Exception e) {
            LOG.error("No se pudieron aplicar las recetas", e);
        }
    }
}
