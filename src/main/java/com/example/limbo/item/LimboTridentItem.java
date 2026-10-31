package com.example.limbo.item;

import com.example.limbo.LimboDimension;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Tridente que SOLO se puede usar (lanzar / Riptide) dentro del Limbo. */
public class LimboTridentItem extends TridentItem {

    public LimboTridentItem() {
        super(new Item.Settings()
                .rarity(Rarity.EPIC)
                .fireproof()
                .maxDamage(250)
                .attributeModifiers(TridentItem.createAttributeModifiers())
                .component(DataComponentTypes.TOOL, TridentItem.createToolComponent()));
    }

    public static boolean canUseIn(World world) {
        return LimboDimension.isLimbo(world);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!canUseIn(world)) return TypedActionResult.fail(user.getStackInHand(hand)); // nunca empieza a cargar
        return super.use(world, user, hand);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!canUseIn(world)) return; // seguridad extra (p.ej. cambio de dimensión cargando)
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
    }
}
