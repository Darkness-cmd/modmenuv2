package com.desafio.menu.server;

import com.desafio.menu.data.*;
import com.desafio.menu.networking.ServerNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.UUID;

/** Actualiza el progreso: obtain (inventario), visit (dimensión), kill (evento). "custom" se avanza con /desafio add. */
public final class QuestTracker {
    private static int tick;
    private QuestTracker() {}

    private static boolean matches(String spec, Identifier id) {
        if (spec == null || spec.isBlank()) return false;
        return spec.contains(":") ? id.toString().equals(spec) : id.getPath().equals(spec);
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tick % 6000 == 0) ProgressStore.save();   // autoguardado cada 5 min
            if (tick % 20 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) scan(p);
            for (UUID id : new ArrayList<>(ProgressStore.dirty)) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                if (p != null) ServerNetworking.sync(p);
            }
            ProgressStore.dirty.clear();
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (!(entity instanceof ServerPlayerEntity p)) return;
            Identifier id = Registries.ENTITY_TYPE.getId(killed.getType());
            PlayerData d = ProgressStore.get(p.getUuid());
            for (Content.Quest q : ContentStore.get().quests) {
                if ("kill".equals(q.type) && matches(q.target, id) && QuestLogic.state(d, q) == QuestLogic.State.ACTIVE)
                    ProgressStore.add(p, q.id, 1);
            }
            for (Content.Mob m : ContentStore.get().bestiary) {
                if ("kill".equalsIgnoreCase(m.discover) && matches(m.entity.isBlank() ? m.id : m.entity, id))
                    ProgressStore.discover(p, m.id);
            }
        });
    }

    private static void scan(ServerPlayerEntity p) {
        PlayerData d = ProgressStore.get(p.getUuid());
        for (Content.Quest q : ContentStore.get().quests) {
            if (QuestLogic.state(d, q) != QuestLogic.State.ACTIVE) continue;
            switch (q.type) {
                case "obtain" -> {
                    Identifier id = Identifier.tryParse(q.target.contains(":") ? q.target : "minecraft:" + q.target);
                    if (id == null) break;
                    Item item = Registries.ITEM.get(id);
                    ProgressStore.setAtLeast(p, q, p.getInventory().count(item));
                }
                case "visit" -> {
                    String dim = p.getServerWorld().getRegistryKey().getValue().toString();
                    if (dim.equals(q.target.contains(":") ? q.target : "minecraft:" + q.target))
                        ProgressStore.setAtLeast(p, q, q.goal);
                }
                default -> {}
            }
        }
    }
}
