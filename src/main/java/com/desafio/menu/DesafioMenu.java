package com.desafio.menu;

import com.desafio.menu.data.ContentStore;
import com.desafio.menu.data.ProgressStore;
import com.desafio.menu.networking.*;
import com.desafio.menu.server.DesafioCommand;
import com.desafio.menu.server.QuestTracker;
import com.desafio.menu.server.RecipeApplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class DesafioMenu implements ModInitializer {
    public static final String MOD_ID = "desafio_menu";

    @Override
    public void onInitialize() {
        // Payloads (CustomPayload 1.21): se registran en común para cliente y servidor
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EditPayload.ID, EditPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimPayload.ID, ClaimPayload.CODEC);

        ServerNetworking.init();
        QuestTracker.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ContentStore.load();
            ProgressStore.load();
            RecipeApplier.apply(server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, rm, ok) -> { if (ok) RecipeApplier.apply(server); });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ProgressStore.save());

        CommandRegistrationCallback.EVENT.register(DesafioCommand::register);
    }
}
