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
        DatapackItems.initialize();
        ModArmorItems.initialize();
        ModParticles.initialize();
        RecipeUnlocker.initialize();
        SkyblockPlayerState.initialize();
        SkyblockAbilityHandler.initialize();
        DamageNumberHandler.initialize();
        LOGGER.info("Registered the migrated SkyBlock item, recipe, stat, and ability systems. Holy Dragon content is excluded.");
    }
}
