package com.desafio.menu.networking;

import com.desafio.menu.DesafioMenu;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S: reclamar recompensas de una misión completada. */
public record ClaimPayload(String questId) implements CustomPayload {
    public static final Id<ClaimPayload> ID = new Id<>(Identifier.of(DesafioMenu.MOD_ID, "claim"));
    public static final PacketCodec<RegistryByteBuf, ClaimPayload> CODEC =
            PacketCodecs.string(128).xmap(ClaimPayload::new, ClaimPayload::questId).cast();

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
