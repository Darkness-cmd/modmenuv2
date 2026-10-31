package com.example.limbo;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Constantes y utilidades de "El Limbo". Coinciden con lo que genera LimboStructure. */
public final class LimboDimension {
    public static final RegistryKey<World> KEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(Limbo.MOD_ID, "limbo"));

    /** Spawn dentro del coliseo (suelo en y=40, de pie en y=41), mirando hacia el agujero y la puerta. */
    public static final Vec3d SPAWN = new Vec3d(0.5, 41.0, -18.5);
    public static final float SPAWN_YAW = 0f;

    /** Agujero central: el pozo mide radio 9; se detecta con margen (radio 10) bajo y=36. */
    public static final Vec3d HOLE_CENTER = new Vec3d(0.5, 0, 0.5);
    public static final double HOLE_RADIUS = 10.0;
    public static final double HOLE_TRIGGER_Y = 36.0;

    private LimboDimension() {}

    public static boolean isLimbo(World world) {
        return world != null && world.getRegistryKey() == KEY;
    }
}
