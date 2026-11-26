package dev.hypixelcustomrecipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Native gemstone ore blocks registered for the Hypixel content namespace. */
public final class GemstoneOreBlocks {
    public static final Block RUBY_ORE = register("ruby_gemstone_ore");
    public static final Block AMETHYST_ORE = register("amethyst_gemstone_ore");
    public static final Block AMBER_ORE = register("amber_gemstone_ore");
    public static final Block TOPAZ_ORE = register("topaz_gemstone_ore");
    public static final Block SAPPHIRE_ORE = register("sapphire_gemstone_ore");
    public static final Block JADE_ORE = register("jade_gemstone_ore");
    public static final Block OPAL_ORE = register("opal_gemstone_ore");
    public static final Block JASPER_ORE = register("jasper_gemstone_ore");
    public static final Block RAINBOW_ORE = register("rainbow_gemstone_ore");

    private GemstoneOreBlocks() {}

    private static Block register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath("hypixel", path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Block block = new Block(BlockBehaviour.Properties.of().setId(blockKey).strength(3.0F, 6.0F));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        return block;
    }

    public static void initialize() {}
}
