package dev.hypixelcustomrecipe;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.SimplePolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import java.io.IOException;

/**
 * Placeable gemstone ores using Polymer Blocks' textured full-block models.
 * The custom assets are copied into Polymer's generated server resource pack,
 * so vanilla clients see the supplied gemstone textures instead of fallbacks.
 */
public final class GemstoneOreBlocks {
    public static final Block RUBY_ORE = register("ruby_gemstone_ore");
    public static final Block AMETHYST_ORE = register("amethyst_gemstone_ore");
    public static final Block AMBER_ORE = register("amber_gemstone_ore");
    public static final Block TOPAZ_ORE = register("topaz_gemstone_ore");
    public static final Block SAPPHIRE_ORE = register("sapphire_gemstone_ore");
    public static final Block JADE_ORE = register("jade_gemstone_ore");
    public static final Block OPAL_ORE = register("opal_gemstone_ore");
    public static final Block JASPER_ORE = register("jasper_gemstone_ore");

    private GemstoneOreBlocks() {}

    private static Block register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath("hypixel", path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Identifier modelId = Identifier.fromNamespaceAndPath("hypixel", "block/" + path);

        Block block = new TexturedGemstoneOreBlock(
                BlockBehaviour.Properties.of().setId(blockKey).strength(3.0F, 6.0F),
                modelId
        );
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        return block;
    }

    private static final class TexturedGemstoneOreBlock extends SimplePolymerBlock implements PolymerTexturedBlock {
        private final BlockState texturedState;

        private TexturedGemstoneOreBlock(BlockBehaviour.Properties properties, Identifier modelId) {
            super(properties, Blocks.NOTE_BLOCK);
            BlockState requested = PolymerBlockResourceUtils.requestBlock(
                    BlockModelType.FULL_BLOCK,
                    PolymerBlockModel.of(modelId)
            );
            if (requested == null) {
                throw new IllegalStateException("Polymer ran out of FULL_BLOCK models for " + modelId);
            }
            this.texturedState = requested;
        }

        @Override
        public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
            return this.texturedState;
        }
    }

    public static void initialize() {
        // Copy the existing "hypixel" asset namespace into Polymer's pack. The
        // registered item/block IDs intentionally keep their historical namespace.
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            try {
                var container = FabricLoader.getInstance()
                        .getModContainer(HypixelCustomRecipe.MOD_ID)
                        .orElseThrow(() -> new IllegalStateException("Could not find this mod's resources"));
                var assets = container.findPath("assets/hypixel")
                        .orElseThrow(() -> new IllegalStateException("Missing assets/hypixel resource folder"));
                if (!builder.copyFromPath(assets, "assets/hypixel", true)) {
                    HypixelCustomRecipe.LOGGER.error("Failed to copy gemstone assets into Polymer's resource pack");
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load gemstone resource-pack assets", exception);
            }
        });
    }
}
