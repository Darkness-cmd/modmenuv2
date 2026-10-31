package com.example.limbo.item;

import com.example.limbo.Limbo;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item LIMBO_TRIDENT = Registry.register(
            Registries.ITEM, Identifier.of(Limbo.MOD_ID, "limbo_trident"), new LimboTridentItem());

    private ModItems() {}

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> e.add(LIMBO_TRIDENT));
    }
}
