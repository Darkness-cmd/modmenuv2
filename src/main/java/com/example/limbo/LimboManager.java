package com.example.limbo;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Lógica de entrada y salida del Limbo + mensajes globales. */
public final class LimboManager {
    /** true = al escapar se devuelve la vida máxima que quitó el agua. */
    public static final boolean RESTORE_MAX_HEALTH_ON_ESCAPE = true;

    private LimboManager() {}

    /** Overworld/otra dimensión -> Limbo. */
    public static void sendToLimbo(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null || LimboDimension.isLimbo(player.getWorld())) return;

        ServerWorld limbo = server.getWorld(LimboDimension.KEY);
        if (limbo == null) {
            Limbo.LOGGER.error("La dimensión limbo:limbo no está cargada (revisa data/limbo/dimension/limbo.json)");
            return;
        }

        LimboStructure.ensureBuilt(limbo);                 // 1ª vez: genera coliseo, puentes y torres
        LimboReturnData.get(server).save(player);          // recordar punto exacto

        Vec3d s = LimboDimension.SPAWN;
        player.teleport(limbo, s.x, s.y, s.z, LimboDimension.SPAWN_YAW, 0f);
        resetMotion(player);

        server.getPlayerManager().broadcast(
                Text.empty().append(player.getDisplayName())
                        .append(Text.literal(" fue llevado al limbo"))
                        .formatted(Formatting.DARK_PURPLE), false);
    }

    /** Limbo -> punto exacto de origen. */
    public static void escapeLimbo(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        LimboReturnData.Entry e = LimboReturnData.get(server).remove(player.getUuid());
        ServerWorld target = null;
        if (e != null) target = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, e.dimension()));
        if (target == null) {                               // fallback: spawn del Overworld
            target = server.getOverworld();
            BlockPos sp = target.getSpawnPos();
            e = new LimboReturnData.Entry(target.getRegistryKey().getValue(),
                    sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0f, 0f);
        }

        player.teleport(target, e.x(), e.y(), e.z(), e.yaw(), e.pitch());
        resetMotion(player);

        if (RESTORE_MAX_HEALTH_ON_ESCAPE) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr != null) attr.removeModifier(LimboEvents.WATER_DRAIN_ID);
        }

        server.getPlayerManager().broadcast(
                Text.empty().append(player.getDisplayName())
                        .append(Text.literal(" ha escapado del limbo"))
                        .formatted(Formatting.LIGHT_PURPLE), false);
    }

    private static void resetMotion(ServerPlayerEntity p) {
        p.fallDistance = 0;                 // evita daño por la caída del agujero
        p.setVelocity(Vec3d.ZERO);
        p.velocityModified = true;
    }
}
