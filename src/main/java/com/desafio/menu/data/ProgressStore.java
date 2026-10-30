package com.desafio.menu.data;

import com.google.gson.reflect.TypeToken;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Progreso por jugador: config/desafio_menu/progress.json */
public final class ProgressStore {
    private static Map<String, PlayerData> data = new HashMap<>();
    public static final Set<UUID> dirty = new HashSet<>();

    private ProgressStore() {}

    private static Path file() { return ContentStore.dir().resolve("progress.json"); }

    public static PlayerData get(UUID id) { return data.computeIfAbsent(id.toString(), k -> new PlayerData()); }

    public static void load() {
        data = new HashMap<>();
        if (!Files.exists(file())) return;
        try (Reader r = Files.newBufferedReader(file())) {
            Map<String, PlayerData> m = ContentStore.GSON.fromJson(r, new TypeToken<Map<String, PlayerData>>() {}.getType());
            if (m != null) data = m;
        } catch (Exception ignored) {}
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) { ContentStore.GSON.toJson(data, w); } catch (Exception ignored) {}
    }

    public static Content.Quest quest(String id) {
        for (Content.Quest q : ContentStore.get().quests) if (q.id.equals(id)) return q;
        return null;
    }

    public static void add(ServerPlayerEntity p, String questId, int n) {
        Content.Quest q = quest(questId);
        if (q == null) return;
        PlayerData d = get(p.getUuid());
        d.progress.put(questId, Math.min(q.goal, d.progress.getOrDefault(questId, 0) + n));
        dirty.add(p.getUuid());
    }

    public static void setAtLeast(ServerPlayerEntity p, Content.Quest q, int value) {
        PlayerData d = get(p.getUuid());
        int old = d.progress.getOrDefault(q.id, 0);
        int now = Math.min(q.goal, Math.max(old, value));
        if (now != old) { d.progress.put(q.id, now); dirty.add(p.getUuid()); }
    }

    public static void discover(ServerPlayerEntity p, String mobId) {
        if (get(p.getUuid()).discovered.add(mobId)) dirty.add(p.getUuid());
    }
}
