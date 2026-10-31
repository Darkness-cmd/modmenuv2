package com.example.limbo.mixin.client;

import com.example.limbo.LimboDimension;
import com.example.limbo.item.LimboTridentItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Igual que HandledScreenMixin pero para la hotbar. (Verifica la firma si cambias de versión.) */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderHotbarItem", at = @At("HEAD"))
    private void limbo$paintLockedHotbar(DrawContext context, int x, int y, RenderTickCounter tickCounter,
                                         PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (stack.getItem() instanceof LimboTridentItem
                && !LimboDimension.isLimbo(MinecraftClient.getInstance().world)) {
            context.fill(x, y, x + 16, y + 16, 0x80FF0000);
        }
    }
}
