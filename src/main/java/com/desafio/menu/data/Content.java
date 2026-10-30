package com.desafio.menu.data;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Todo el contenido editable. Cada lista se guarda en su propio JSON dentro de config/desafio_menu/. */
public class Content {
    public List<Quest> quests = new ArrayList<>();
    public List<Guide> guide = new ArrayList<>();
    public List<Lore> lore = new ArrayList<>();
    public List<Mob> bestiary = new ArrayList<>();
    public Recipes recipes = new Recipes();

    public static class Quest {
        public String id = "", title = "", description = "", icon = "minecraft:stone";
        /** kill | obtain | visit | custom */
        public String type = "custom", target = "";
        public int goal = 1;
        /** ids de misiones que deben estar COMPLETADAS (reclamadas) */
        public List<String> requires = new ArrayList<>();
        public List<String> objectives = new ArrayList<>();
        /** "minecraft:gold_ingot*2" */
        public List<String> rewards = new ArrayList<>();
    }

    public static class Guide {
        public String id = "", title = "", text = "", icon = "minecraft:book";
    }

    public static class Lore {
        public String id = "", title = "", text = "", icon = "minecraft:apple";
        /** id de misión que desbloquea esta entrada (vacío = siempre visible) */
        public String unlockedBy = "";
    }

    public static class Mob {
        public String id = "", name = "", description = "", tip = "";
        /** id de EntityType si es distinto de "id" (ej. minecraft:zombie) */
        public String entity = "";
        public int danger = 1;
        public List<String> loot = new ArrayList<>();
        /** kill | always */
        public String discover = "kill";
    }

    public static class Recipes {
        public List<String> removed = new ArrayList<>();
        public List<Custom> custom = new ArrayList<>();
    }

    public static class Custom {
        public String id = "";
        public JsonObject recipe = new JsonObject();
    }
}
