package com.example.limbo;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Guarda (y persiste en disco) el punto exacto desde el que cada jugador fue enviado al Limbo. */
public class LimboReturnData extends PersistentState {
    public record Entry(Identifier dimension, double x, double y, double z, float yaw, float pitch) {}

    private final Map<UUID, Entry> entries = new HashMap<>();
    private boolean built = false;   // ¿ya se generó el coliseo/torres?

    public boolean isBuilt() { return built; }
    public void markBuilt() { built = true; markDirty(); }

    public static final PersistentState.Type<LimboReturnData> TYPE =
            new PersistentState.Type<>(LimboReturnData::new, LimboReturnData::fromNbt, null);

    public static LimboReturnData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, Limbo.MOD_ID + "_return");
    }

    public void save(ServerPlayerEntity p) {
        entries.put(p.getUuid(), new Entry(p.getWorld().getRegistryKey().getValue(),
                p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch()));
        markDirty();
    }

    public Entry remove(UUID id) {
        Entry e = entries.remove(id);
        if (e != null) markDirty();
        return e;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound all = new NbtCompound();
        entries.forEach((uuid, e) -> {
            NbtCompound t = new NbtCompound();
            t.putString("dim", e.dimension().toString());
            t.putDouble("x", e.x());
            t.putDouble("y", e.y());
            t.putDouble("z", e.z());
            t.putFloat("yaw", e.yaw());
            t.putFloat("pitch", e.pitch());
            all.put(uuid.toString(), t);
        });
        nbt.put("entries", all);
        nbt.putBoolean("built", built);
        return nbt;
    }

    public static LimboReturnData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        LimboReturnData data = new LimboReturnData();
        data.built = nbt.getBoolean("built");
        NbtCompound all = nbt.getCompound("entries");
        for (String key : all.getKeys()) {
            NbtCompound t = all.getCompound(key);
            data.entries.put(UUID.fromString(key), new Entry(Identifier.of(t.getString("dim")),
                    t.getDouble("x"), t.getDouble("y"), t.getDouble("z"),
                    t.getFloat("yaw"), t.getFloat("pitch")));
        }
        return data;
    }
}
