package com.desafio.menu.networking;

import com.desafio.menu.DesafioMenu;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** S2C: contenido + progreso del jugador (JSON). Límite de paquete S2C ≈ 1 MiB. */
public record SyncPayload(String json) implements CustomPayload {
    public static final Id<SyncPayload> ID = new Id<>(Identifier.of(DesafioMenu.MOD_ID, "sync"));
    public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC =
            PacketCodecs.string(300_000).xmap(SyncPayload::new, SyncPayload::json).cast();

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
