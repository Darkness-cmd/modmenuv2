package com.desafio.menu.data;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class ItemSpec {
    private ItemSpec() {}

    /** "minecraft:gold_ingot*2" | "gold_ingot" -> ItemStack (BARRIER si no existe). */
    public static ItemStack parse(String spec) {
        if (spec == null || spec.isBlank()) return new ItemStack(Items.BARRIER);
        int n = 1;
        String id = spec.trim();
        int i = id.indexOf('*');
        if (i > 0) {
            try { n = Math.max(1, Integer.parseInt(id.substring(i + 1).trim())); } catch (NumberFormatException ignored) {}
            id = id.substring(0, i).trim();
        }
        Identifier ident = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
        Item item = ident == null ? Items.BARRIER : Registries.ITEM.getOrEmpty(ident).orElse(Items.BARRIER);
        return new ItemStack(item, n);
    }
}
