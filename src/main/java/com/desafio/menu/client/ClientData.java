package com.desafio.menu.client;

import com.desafio.menu.data.Content;
import com.desafio.menu.data.ContentStore;
import com.desafio.menu.data.PlayerData;
import com.desafio.menu.data.SyncData;
import net.minecraft.client.MinecraftClient;

/** Copia local (solo lectura) de lo que manda el servidor. */
public final class ClientData {
    public interface Refreshable { void refresh(); }

    public static Content content = new Content();
    public static PlayerData me = new PlayerData();

    private ClientData() {}

    public static void apply(String json) {
        try {
            SyncData d = ContentStore.COMPACT.fromJson(json, SyncData.class);
            content = d.content != null ? d.content : new Content();
            me = d.me != null ? d.me : new PlayerData();
        } catch (Exception e) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen instanceof Refreshable r) r.refresh();
    }

    public static void reset() {
        content = new Content();
        me = new PlayerData();
    }
}
