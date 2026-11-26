package dev.hypixelcustomrecipe;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Attaches gemstone ore features to End biomes. */
public final class GemstoneWorldGeneration {
    private static final ResourceKey<PlacedFeature> RUBY = key("ruby_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> AMETHYST = key("amethyst_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> AMBER = key("amber_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> TOPAZ = key("topaz_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> SAPPHIRE = key("sapphire_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> JADE = key("jade_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> OPAL = key("opal_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> JASPER = key("jasper_gemstone_ore_placed");
    private static final ResourceKey<PlacedFeature> RAINBOW = key("rainbow_gemstone_ore_placed");

    private GemstoneWorldGeneration() {}

    private static ResourceKey<PlacedFeature> key(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("hypixel", path));
    }

    public static void initialize() {
        Predicate<BiomeSelectionContext> endBiomes = BiomeSelectors.foundInTheEnd();
        add(endBiomes, RUBY);
        add(endBiomes, AMETHYST);
        add(endBiomes, AMBER);
        add(endBiomes, TOPAZ);
        add(endBiomes, SAPPHIRE);
        add(endBiomes, JADE);
        add(endBiomes, OPAL);
        add(endBiomes, JASPER);
        add(endBiomes, RAINBOW);
    }

    private static void add(Predicate<BiomeSelectionContext> selector, ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(selector, GenerationStep.Decoration.UNDERGROUND_ORES, feature);
    }
}
