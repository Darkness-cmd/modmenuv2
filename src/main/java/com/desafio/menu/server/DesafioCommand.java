package com.desafio.menu.server;

import com.desafio.menu.data.ContentStore;
import com.desafio.menu.data.ProgressStore;
import com.desafio.menu.networking.ServerNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class DesafioCommand {
    private DesafioCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> d, CommandRegistryAccess reg, CommandManager.RegistrationEnvironment env) {
        d.register(CommandManager.literal("desafio").requires(s -> s.hasPermissionLevel(2))
            .then(CommandManager.literal("add")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("quest", StringArgumentType.word())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
                            ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
                            ProgressStore.add(p, StringArgumentType.getString(ctx, "quest"), IntegerArgumentType.getInteger(ctx, "amount"));
                            ctx.getSource().sendFeedback(() -> Text.literal("Progreso actualizado."), true);
                            return 1;
                        })))))
            .then(CommandManager.literal("reload").executes(ctx -> {
                ContentStore.load();
                RecipeApplier.apply(ctx.getSource().getServer());
                ServerNetworking.syncAll(ctx.getSource().getServer());
                ctx.getSource().sendFeedback(() -> Text.literal("JSON recargados desde config/desafio_menu."), true);
                return 1;
            })));
    }
}
