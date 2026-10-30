package com.desafio.menu.data;

import java.util.ArrayList;
import java.util.List;

/** Contenido de ejemplo (el que se ve en el video). Solo se escribe si el JSON no existe. */
final class Defaults {
    private Defaults() {}

    static List<Content.Quest> quests() {
        List<Content.Quest> l = new ArrayList<>();
        Content.Quest a = new Content.Quest();
        a.id = "primer_dia"; a.title = "Primer día"; a.description = "Sobrevive a la primera noche. Usa [[/desafio add]] para avanzar.";
        a.icon = "minecraft:wooden_sword"; a.type = "custom"; a.goal = 5;
        a.rewards.add("minecraft:stone_pickaxe"); a.rewards.add("minecraft:book");
        l.add(a);

        Content.Quest b = new Content.Quest();
        b.id = "fiebre_hierro"; b.title = "Fiebre del hierro"; b.description = "Consigue [[Lingotes de Hierro]]. Son muy importantes para sobrevivir.";
        b.icon = "minecraft:iron_ingot"; b.type = "obtain"; b.target = "minecraft:iron_ingot"; b.goal = 10;
        b.requires.add("primer_dia"); b.rewards.add("minecraft:iron_pickaxe"); b.rewards.add("minecraft:book");
        l.add(b);

        Content.Quest c = new Content.Quest();
        c.id = "bienvenido_nether"; c.title = "Bienvenido al Nether"; c.description = "Cruza el portal. No mires atrás.";
        c.icon = "minecraft:obsidian"; c.type = "visit"; c.target = "minecraft:the_nether"; c.goal = 1;
        c.requires.add("fiebre_hierro"); c.objectives.add("Visitar minecraft:the_nether");
        c.rewards.add("minecraft:gold_ingot*2");
        l.add(c);
        return l;
    }

    static List<Content.Guide> guide() {
        List<Content.Guide> l = new ArrayList<>();
        Content.Guide g = new Content.Guide();
        g.id = "lava_purificadora"; g.title = "Lava purificadora";
        g.text = "La lava ahora quita el efecto de resistencia al fuego.";
        g.icon = "minecraft:lava_bucket";
        l.add(g);
        return l;
    }

    static List<Content.Lore> lore() {
        List<Content.Lore> l = new ArrayList<>();
        Content.Lore e = new Content.Lore();
        e.id = "capitulo_1"; e.title = "Capítulo 1"; e.text = "Nadie recuerda cómo empezó todo...";
        e.icon = "minecraft:golden_apple"; e.unlockedBy = "primer_dia";
        l.add(e);
        return l;
    }

    private static Content.Mob mob(String id, String name, int danger, String desc, String tip, String discover, String... loot) {
        Content.Mob m = new Content.Mob();
        m.id = id; m.name = name; m.danger = danger; m.description = desc; m.tip = tip; m.discover = discover;
        for (String s : loot) m.loot.add(s);
        return m;
    }

    static List<Content.Mob> bestiary() {
        List<Content.Mob> l = new ArrayList<>();
        l.add(mob("zombie", "Zombie", 1, "Muerto viviente que aparece de noche y en la oscuridad. Ataca en grupo.",
                "Pelea con la espalda contra una pared y no te dejes rodear. Una [[espada]] basta.", "kill", "minecraft:rotten_flesh"));
        l.add(mob("creeper", "Creeper", 3, "Se acerca en silencio y explota.", "Golpea y retrocede. Nunca lo dejes acercarse.", "kill", "minecraft:gunpowder"));
        l.add(mob("skeleton", "Skeleton", 2, "Arquero no muerto.", "Acércate en zigzag o usa un [[escudo]].", "kill", "minecraft:bone", "minecraft:arrow"));
        l.add(mob("enderman", "Enderman", 4, "Alto y teletransportador. No lo mires a los ojos.", "Usa una calabaza en la cabeza.", "kill", "minecraft:ender_pearl"));
        Content.Mob p = mob("ss", "Fantasma rojo", 5, "Un fantasma de color rojo.",
                "Va por ti hasta matarte de manera rápida; la única forma de matarlo es con la [[espada]] fantasmal.", "always", "minecraft:barrier");
        p.entity = "minecraft:phantom";
        l.add(p);
        return l;
    }
}
