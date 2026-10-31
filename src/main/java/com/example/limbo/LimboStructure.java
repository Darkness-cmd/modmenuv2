package com.example.limbo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Genera por código (una sola vez) el coliseo destruido con pozo central, el puente roto,
 * y 6 torres estilo castillo unidas por puentes rotos sobre el agua morada.
 *
 * Mundo plano (ver dimension/limbo.json): y0 bedrock, y1-3 blackstone, y4-19 agua.
 * Todo lo construido está a y=40 (puentes y suelo del coliseo).
 */
public final class LimboStructure {
    private static final int WATER_BOTTOM = 4;
    private static final int FLOOR_Y = 40;
    private static final int BASE_R = 28;          // radio del coliseo
    private static final int HOLE_R = 9;           // radio del agujero
    private static final int WALL_INNER_R = 25;
    private static final int TOWER_HALF = 5;       // torres de 11x11
    // Hexágono de torres a radio 80 (la primera, en +Z, es la del puente principal)
    private static final int[][] TOWERS = {{0, 80}, {-69, 40}, {-69, -40}, {0, -80}, {69, -40}, {69, 40}};

    private LimboStructure() {}

    public static void ensureBuilt(ServerWorld world) {
        LimboReturnData data = LimboReturnData.get(world.getServer());
        if (data.isBuilt()) return;
        Limbo.LOGGER.info("Generando El Limbo (coliseo, puentes y torres)...");
        build(world);
        data.markBuilt();
        Limbo.LOGGER.info("El Limbo generado.");
    }

    private static void build(ServerWorld w) {
        Random r = new Random(1337L);   // misma estructura siempre
        coliseum(w, r);
        for (int[] t : TOWERS) tower(w, t[0], t[1], 58 + r.nextInt(10), r);
        // Puerta principal -> primera torre (puente muy roto, primeros 8 bloques intactos)
        bridge(w, 0, BASE_R + 1, TOWERS[0][0], TOWERS[0][1], 2, 0.08, 8, r);
        // Puentes entre torres (anillo)
        for (int i = 0; i < TOWERS.length; i++) {
            int[] a = TOWERS[i], b = TOWERS[(i + 1) % TOWERS.length];
            bridge(w, a[0], a[1], b[0], b[1], 1, 0.05, 0, r);
        }
    }

    // ---------------------------------------------------------------- COLISEO
    private static void coliseum(ServerWorld w, Random r) {
        // 1) Base maciza + suelo, con el pozo central sin tocar (queda aire arriba y agua abajo)
        for (int x = -BASE_R; x <= BASE_R; x++) {
            for (int z = -BASE_R; z <= BASE_R; z++) {
                int d2 = x * x + z * z;
                if (d2 > BASE_R * BASE_R || d2 < HOLE_R * HOLE_R) continue;
                for (int y = WATER_BOTTOM; y < FLOOR_Y; y++) set(w, x, y, z, brick(r));
                set(w, x, FLOOR_Y, z, r.nextInt(60) == 0 ? Blocks.CRYING_OBSIDIAN.getDefaultState() : brick(r));
            }
        }

        // 2) Muro exterior destruido: 36 sectores con altura aleatoria (0 = derrumbado)
        int[] sector = new int[36];
        for (int i = 0; i < 36; i++) sector[i] = r.nextInt(100) < 25 ? 0 : 3 + r.nextInt(9);
        for (int x = -BASE_R; x <= BASE_R; x++) {
            for (int z = -BASE_R; z <= BASE_R; z++) {
                int d2 = x * x + z * z;
                if (d2 < WALL_INNER_R * WALL_INNER_R || d2 > BASE_R * BASE_R) continue;
                if (z > 0 && Math.abs(x) <= 3) continue;                  // PUERTA PRINCIPAL (+Z)
                double ang = Math.atan2(z, x);
                int s = (int) Math.floor((ang + Math.PI) / (2 * Math.PI) * 36) % 36;
                int h = sector[s];
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + h; y++) {
                    if (y > FLOOR_Y + h - 3 && r.nextDouble() < 0.3) continue;   // bordes desgastados
                    set(w, x, y, z, brick(r));
                }
            }
        }

        // 3) Pilares que flanquean la puerta
        for (int sign : new int[]{-1, 1}) {
            for (int ox = 4; ox <= 5; ox++) {
                for (int z = 24; z <= 26; z++) {
                    for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 14; y++) set(w, sign * ox, y, z, brick(r));
                }
            }
            set(w, sign * 4, FLOOR_Y + 15, 25, Blocks.SOUL_LANTERN.getDefaultState());
        }

        // 4) Columnas rotas dentro del coliseo (anillo a radio 18)
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(15 + 30 * k);
            int px = (int) Math.round(18 * Math.cos(a)), pz = (int) Math.round(18 * Math.sin(a));
            int h = 3 + r.nextInt(7);
            for (int ox = 0; ox <= 1; ox++) for (int oz = 0; oz <= 1; oz++)
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + h; y++) set(w, px + ox, y, pz + oz, brick(r));
            if (r.nextInt(3) == 0) set(w, px, FLOOR_Y + h + 1, pz, Blocks.SOUL_LANTERN.getDefaultState());
        }
    }

    // ---------------------------------------------------------------- TORRES
    private static void tower(ServerWorld w, int cx, int cz, int topY, Random r) {
        for (int dx = -TOWER_HALF; dx <= TOWER_HALF; dx++) {
            for (int dz = -TOWER_HALF; dz <= TOWER_HALF; dz++) {
                int d = Math.max(Math.abs(dx), Math.abs(dz));
                // cimientos macizos desde el fondo hasta el suelo (y=40)
                for (int y = WATER_BOTTOM; y <= FLOOR_Y; y++) set(w, cx + dx, y, cz + dz, brick(r));
                if (d < 4) continue;                                        // interior hueco
                for (int y = FLOOR_Y + 1; y <= topY; y++) {
                    if (y > topY - 4 && r.nextDouble() < 0.12) continue;    // muro desgastado
                    set(w, cx + dx, y, cz + dz, brick(r));
                }
                if (Math.floorMod(dx + dz, 2) == 0) set(w, cx + dx, topY + 1, cz + dz, brick(r)); // almenas
                if (Math.abs(dx) == TOWER_HALF && Math.abs(dz) == TOWER_HALF)
                    set(w, cx + dx, topY + 2, cz + dz, Blocks.SOUL_LANTERN.getDefaultState());
            }
        }
    }

    // ---------------------------------------------------------------- PUENTES
    /** Puente recto a y=40 con tramos caídos aleatorios, soportes cada ~20 bloques y puertas al entrar a torres. */
    private static void bridge(ServerWorld w, int x1, int z1, int x2, int z2,
                               int half, double gapChance, int safeBlocks, Random r) {
        double dx = x2 - x1, dz = z2 - z1;
        int steps = (int) (Math.hypot(dx, dz) * 2);
        int gap = 0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(x1 + dx * t);
            int z = (int) Math.round(z1 + dz * t);

            if (insideAnyTower(x, z)) { carveDoor(w, x, z); continue; }
            if (gap > 0) { gap--; continue; }
            if (i > safeBlocks * 2 && i % 2 == 0 && r.nextDouble() < gapChance) { gap = 6 + r.nextInt(8); continue; }

            for (int ox = -half; ox <= half; ox++)
                for (int oz = -half; oz <= half; oz++)
                    set(w, x + ox, FLOOR_Y, z + oz, brick(r));

            if (i % 40 == 0)                                               // soporte hasta el fondo
                for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++)
                    for (int y = WATER_BOTTOM; y < FLOOR_Y; y++) set(w, x + ox, y, z + oz, brick(r));
        }
    }

    private static boolean insideAnyTower(int x, int z) {
        for (int[] t : TOWERS)
            if (Math.abs(x - t[0]) <= TOWER_HALF && Math.abs(z - t[1]) <= TOWER_HALF) return true;
        return false;
    }

    private static void carveDoor(ServerWorld w, int x, int z) {
        for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++)
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 3; y++) set(w, x + ox, y, z + oz, Blocks.AIR.getDefaultState());
    }

    // ---------------------------------------------------------------- UTIL
    private static BlockState brick(Random r) {
        int n = r.nextInt(10);
        if (n < 4) return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        if (n < 7) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        if (n < 9) return Blocks.BLACKSTONE.getDefaultState();
        return Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
    }

    private static void set(ServerWorld w, int x, int y, int z, BlockState s) {
        w.setBlockState(new BlockPos(x, y, z), s, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }
}
