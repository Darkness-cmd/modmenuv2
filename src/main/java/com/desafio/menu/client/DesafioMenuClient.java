package com.desafio.menu.client;

import com.desafio.menu.networking.SyncPayload;
import com.desafio.menu.screen.DesafioScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DesafioMenuClient implements ClientModInitializer {
    public static KeyBinding OPEN_KEY;

    @Override
    public void onInitializeClient() {
        // Tecla G (KeyBindingHelper = el "FabricKeyBindingHelper" de Fabric API 1.21)
        OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.desafio_menu.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.desafio_menu"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.wasPressed()) {
                if (client.currentScreen == null && client.player != null) client.setScreen(new DesafioScreen());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, ctx) -> ClientData.apply(payload.json()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientData.reset());
    }
}
