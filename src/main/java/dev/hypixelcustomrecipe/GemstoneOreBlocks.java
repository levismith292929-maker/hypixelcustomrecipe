package dev.hypixelcustomrecipe;

import eu.pb4.polymer.core.api.block.SimplePolymerBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Placeable gemstone ores. Polymer displays each custom block as its matching
 * vanilla stained-glass color for players without the mod.
 */
public final class GemstoneOreBlocks {
    public static final Block RUBY_ORE = register("ruby_gemstone_ore", vanillaBlock("red_stained_glass"));
    public static final Block AMETHYST_ORE = register("amethyst_gemstone_ore", vanillaBlock("purple_stained_glass"));
    public static final Block AMBER_ORE = register("amber_gemstone_ore", vanillaBlock("orange_stained_glass"));
    public static final Block TOPAZ_ORE = register("topaz_gemstone_ore", vanillaBlock("yellow_stained_glass"));
    public static final Block SAPPHIRE_ORE = register("sapphire_gemstone_ore", vanillaBlock("blue_stained_glass"));
    public static final Block JADE_ORE = register("jade_gemstone_ore", vanillaBlock("lime_stained_glass"));
    public static final Block OPAL_ORE = register("opal_gemstone_ore", vanillaBlock("light_gray_stained_glass"));
    public static final Block JASPER_ORE = register("jasper_gemstone_ore", vanillaBlock("brown_stained_glass"));

    private GemstoneOreBlocks() {}

    private static Block vanillaBlock(String path) {
        return BuiltInRegistries.BLOCK.get(Identifier.fromNamespaceAndPath("minecraft", path))
                .orElseThrow(() -> new IllegalStateException("Missing vanilla block: " + path)).value();
    }

    private static Block register(String path, Block clientAppearance) {
        Identifier id = Identifier.fromNamespaceAndPath("hypixel", path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Block block = new SimplePolymerBlock(
                BlockBehaviour.Properties.of().setId(blockKey).strength(3.0F, 6.0F),
                clientAppearance
        );
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        return block;
    }

    public static void initialize() {
        // Triggers registration of all eight placeable gemstone ores.
    }
}
