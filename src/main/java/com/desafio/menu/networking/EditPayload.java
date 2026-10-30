package com.desafio.menu.networking;

import com.desafio.menu.DesafioMenu;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: una sola edición (el límite C2S es 32 767 bytes, por eso se envía por entrada y no el JSON completo).
 * category: quests | guide | lore | bestiary | recipes
 * op: upsert | delete | remove_recipe | restore_recipe
 */
public record EditPayload(String category, String op, String json) implements CustomPayload {
    public static final Id<EditPayload> ID = new Id<>(Identifier.of(DesafioMenu.MOD_ID, "edit"));
    public static final PacketCodec<RegistryByteBuf, EditPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(32), EditPayload::category,
            PacketCodecs.string(32), EditPayload::op,
            PacketCodecs.string(9_000), EditPayload::json,
            EditPayload::new).cast();

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
