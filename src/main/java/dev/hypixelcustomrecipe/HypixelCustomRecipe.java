package dev.hypixelcustomrecipe;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HypixelCustomRecipe implements ModInitializer {
    public static final String MOD_ID = "hypixelcustomrecipe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        LOGGER.info("Registered 8 Dragon Fragments, the Necron Core, and the Aspect of the End.");
    }
}
