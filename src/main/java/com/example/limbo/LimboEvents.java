package com.example.limbo;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;

public final class LimboEvents {
    /** Modificador persistente de vida máxima (se guarda con el jugador). */
    public static final Identifier WATER_DRAIN_ID = Identifier.of(Limbo.MOD_ID, "purple_water_drain");
    /** Vida máxima que se pierde cada segundo en el agua (2.0 = 1 corazón). */
    private static final double DRAIN_PER_SECOND = 1.0;   // medio corazón/seg
    /** La vida máxima nunca baja de esto (2.0 = 1 corazón). */
    private static final double MIN_MAX_HEALTH = 2.0;

    private LimboEvents() {}

    public static void register() {
        registerBedAndBlockRules();
        registerLimboTick();
    }

    /** Un creativo con permisos (op) puede construir en el Limbo. Quita esto si quieres bloqueo total. */
    private static boolean canBypass(PlayerEntity p) {
        return p.isCreativeLevelTwoOp();
    }

    // ------------------------------------------------------------------ CAMAS + PROTECCIÓN
    private static void registerBedAndBlockRules() {

        // 1) Usar cama / colocar bloques
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            BlockState state = world.getBlockState(hit.getBlockPos());

            if (state.getBlock() instanceof BedBlock) {
                if (LimboDimension.isLimbo(world)) return ActionResult.FAIL;       // sin explosión ni sueño
                if (world.getRegistryKey() != World.OVERWORLD) {                   // Nether, End, otras...
                    if (player instanceof ServerPlayerEntity sp) LimboManager.sendToLimbo(sp);
                    return ActionResult.SUCCESS;                                   // cancela BedBlock#onUse
                }
                return ActionResult.PASS;                                          // Overworld: dormir normal
            }

            if (LimboDimension.isLimbo(world) && !canBypass(player)) {
                Item held = player.getStackInHand(hand).getItem();
                if (held instanceof BlockItem || held instanceof BucketItem) return ActionResult.FAIL; // no colocar
            }
            return ActionResult.PASS;
        });

        // 2) No romper bloques en el Limbo
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) ->
                !LimboDimension.isLimbo(world) || canBypass(player));

        // 3) Romper una cama (fuera del Limbo) -> Limbo
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (state.getBlock() instanceof BedBlock
                    && !LimboDimension.isLimbo(world)
                    && player instanceof ServerPlayerEntity sp) {
                LimboManager.sendToLimbo(sp);
            }
        });
    }

    // ------------------------------------------------------------------ TICK: AGUJERO + AGUA
    private static void registerLimboTick() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!LimboDimension.isLimbo(world)) return;

            // Copia: teleportar saca al jugador de la lista mientras iteramos
            for (ServerPlayerEntity p : new ArrayList<>(world.getPlayers())) {
                if (p.isSpectator()) continue;

                if (fellInHole(p) || p.getY() < world.getBottomY() - 8) {
                    LimboManager.escapeLimbo(p);
                    continue;
                }
                if (world.getTime() % 20 == 0 && p.isTouchingWater()) {
                    drainMaxHealth(p);
                }
            }
        });
    }

    private static boolean fellInHole(ServerPlayerEntity p) {
        double dx = p.getX() - LimboDimension.HOLE_CENTER.x;
        double dz = p.getZ() - LimboDimension.HOLE_CENTER.z;
        return dx * dx + dz * dz <= LimboDimension.HOLE_RADIUS * LimboDimension.HOLE_RADIUS
                && p.getY() < LimboDimension.HOLE_TRIGGER_Y;
    }

    /** Resta vida máxima de forma acumulativa con un AttributeModifier persistente. */
    private static void drainMaxHealth(ServerPlayerEntity p) {
        EntityAttributeInstance attr = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        if (p.getMaxHealth() - DRAIN_PER_SECOND < MIN_MAX_HEALTH) return;   // tope mínimo

        EntityAttributeModifier old = attr.getModifier(WATER_DRAIN_ID);
        double accumulated = old == null ? 0.0 : old.value();

        attr.removeModifier(WATER_DRAIN_ID);
        attr.addPersistentModifier(new EntityAttributeModifier(
                WATER_DRAIN_ID, accumulated - DRAIN_PER_SECOND, EntityAttributeModifier.Operation.ADD_VALUE));

        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
}
