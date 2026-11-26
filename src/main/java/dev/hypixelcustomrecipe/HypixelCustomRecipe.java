package dev.hypixelcustomrecipe;

import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEffects;
import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenMobs;
import dev.hypixelcustomrecipe.entity.voidridden.VoidTrailManager;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HypixelCustomRecipe implements ModInitializer {
    public static final String MOD_ID = "hypixelcustomrecipe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // The custom item models and textures live in assets/hypixel/.
        // Register that namespace so Polymer includes them in its generated pack.
        PolymerResourcePackUtils.addModAssets("hypixel");

        ModItems.initialize();
        DatapackItems.initialize();
        GemstoneItems.initialize();
        GemstoneCoreItems.initialize();
        GemstoneAccessoryItems.initialize();
        GemstoneOreBlocks.initialize();
        GemstoneWorldGeneration.initialize();
        GemstoneOreMiningHandler.initialize();
        MiningExperienceHandler.initialize();
        IronAlloyDropHandler.initialize();
        ModArmorItems.initialize();
        ModCreativeTab.initialize();
        RecipeUnlocker.initialize();
        SkyblockPlayerState.initialize();
        SkyblockAbilityHandler.initialize();
        NecronGuardHandler.initialize();
        DamageNumberHandler.initialize();
        VoidriddenEffects.initialize();
        VoidriddenMobs.initialize();
        VoidTrailManager.initialize();
        LOGGER.info("Registered the migrated SkyBlock item, recipe, stat, and ability systems. Holy Dragon content is excluded.");
    }
}
