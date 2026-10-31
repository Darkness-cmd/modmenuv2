package com.example.limbo.mixin.client;

import com.example.limbo.LimboDimension;
import com.example.limbo.item.LimboTridentItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pinta de rojo el slot (inventario, cofres...) con el tridente bloqueado. El ítem se puede mover normal. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void limbo$paintLockedSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        if (slot.getStack().getItem() instanceof LimboTridentItem
                && !LimboDimension.isLimbo(MinecraftClient.getInstance().world)) {
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FF0000);
        }
    }
}
