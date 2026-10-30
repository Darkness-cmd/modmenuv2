package com.desafio.menu.networking;

import com.desafio.menu.data.*;
import com.desafio.menu.server.RecipeApplier;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ServerNetworking {
    private ServerNetworking() {}

    public static boolean canEdit(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        return p.hasPermissionLevel(2) || (s != null && s.isHost(p.getGameProfile()));
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));

        ServerPlayNetworking.registerGlobalReceiver(EditPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            MinecraftServer server = ctx.server();
            if (!canEdit(p)) {
                p.sendMessage(Text.literal("No tienes permiso para usar el editor.").formatted(Formatting.RED), true);
                return;
            }
            if (ContentStore.apply(payload.category(), payload.op(), payload.json())) {
                if (payload.category().equals("recipes")) RecipeApplier.apply(server);
                syncAll(server);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ClaimPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            Content.Quest q = ProgressStore.quest(payload.questId());
            PlayerData d = ProgressStore.get(p.getUuid());
            if (q == null || QuestLogic.state(d, q) != QuestLogic.State.READY) return;
            d.claimed.add(q.id);
            for (String r : q.rewards) {
                ItemStack s = ItemSpec.parse(r);
                if (!p.giveItemStack(s)) p.dropItem(s, false);
            }
            ProgressStore.dirty.add(p.getUuid());
            sync(p);
        });
    }

    public static void sync(ServerPlayerEntity p) {
        SyncData d = new SyncData(ContentStore.get(), ProgressStore.get(p.getUuid()));
        ServerPlayNetworking.send(p, new SyncPayload(ContentStore.COMPACT.toJson(d)));
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayerEntity p : PlayerLookup.all(server)) sync(p);
    }
}
