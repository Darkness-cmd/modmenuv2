package com.example.limbo.client;

import com.example.limbo.LimboDimension;
import com.example.limbo.item.LimboTridentItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LimboClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.getItem() instanceof LimboTridentItem
                    && !LimboDimension.isLimbo(MinecraftClient.getInstance().world)) {
                lines.add(Text.translatable("item.limbo.limbo_trident.locked").formatted(Formatting.RED));
            }
        });
    }
}
