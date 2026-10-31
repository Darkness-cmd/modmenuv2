package com.example.limbo;

import com.example.limbo.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Limbo implements ModInitializer {
    public static final String MOD_ID = "limbo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.init();
        LimboEvents.register();
    }
}
