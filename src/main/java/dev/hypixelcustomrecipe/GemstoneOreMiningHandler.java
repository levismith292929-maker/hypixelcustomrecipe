package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class GemstoneOreMiningHandler {
    private GemstoneOreMiningHandler() {}

    public static void initialize() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            int oreTier = getOreTier(state);
            if (oreTier < 0 || player.getAbilities().instabuild) {
                return true;
            }

            int toolTier = getToolTier(player.getMainHandItem());
            if (toolTier >= oreTier) {
                return true;
            }

            player.displayClientMessage(
                    Component.literal("You need a stronger mining tool to mine this gemstone."),
                    true
            );
            return false;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            int oreTier = getOreTier(state);
            if (oreTier < 0 || player.getAbilities().instabuild
                    || !(level instanceof ServerLevel serverLevel)) {
                return;
            }

            ItemStack tool = player.getMainHandItem();
            if (getToolTier(tool) < oreTier) {
                return;
            }

            Item drop = getRoughGemstone(state);
            if (drop == null) {
                return;
            }

            int fortune = EnchantmentHelper.getItemEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.FORTUNE),
                    tool
            );
            fortune = Math.min(fortune, 3);

            // Each tier has a random base amount. Every level also has an
            // independent 20% chance to add one extra gemstone.
            int amount = switch (fortune) {
                case 1 -> 2 + level.getRandom().nextInt(2); // 2-3, plus 20% for 4
                case 2 -> 3 + level.getRandom().nextInt(3); // 3-5, plus 20% for 6
                case 3 -> 4 + level.getRandom().nextInt(4); // 4-7, plus 20% for 8
                default -> 1;                              // 1, plus 20% for 2
            };
            if (level.getRandom().nextFloat() < 0.20F) {
                amount++;
            }

            ItemEntity gemstone = new ItemEntity(
                    serverLevel,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    new ItemStack(drop, amount)
            );
            gemstone.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(gemstone);
        });
    }

    /**
     * Mining tiers: netherite pickaxe = 0, then Ruby through Jasper drills = 1-8.
     * Ore tiers use the same numbering, so Ruby requires a netherite pickaxe,
     * Amethyst requires Ruby Drill, and each later ore requires the previous drill.
     */
    private static int getToolTier(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.NETHERITE_PICKAXE) return 0;
        if (item == DatapackItems.RUBY_DRILL) return 1;
        if (item == DatapackItems.AMETHYST_DRILL) return 2;
        if (item == DatapackItems.AMBER_DRILL) return 3;
        if (item == DatapackItems.TOPAZ_DRILL) return 4;
        if (item == DatapackItems.SAPPHIRE_DRILL) return 5;
        if (item == DatapackItems.JADE_DRILL) return 6;
        if (item == DatapackItems.OPAL_DRILL) return 7;
        if (item == DatapackItems.JASPER_DRILL) return 8;
        return -1;
    }

    private static int getOreTier(BlockState state) {
        if (state.is(GemstoneOreBlocks.RUBY_ORE)) return 0;
        if (state.is(GemstoneOreBlocks.AMETHYST_ORE)) return 1;
        if (state.is(GemstoneOreBlocks.AMBER_ORE)) return 2;
        if (state.is(GemstoneOreBlocks.TOPAZ_ORE)) return 3;
        if (state.is(GemstoneOreBlocks.SAPPHIRE_ORE)) return 4;
        if (state.is(GemstoneOreBlocks.JADE_ORE)) return 5;
        if (state.is(GemstoneOreBlocks.OPAL_ORE)) return 6;
        if (state.is(GemstoneOreBlocks.JASPER_ORE)) return 7;
        return -1;
    }

    private static Item getRoughGemstone(BlockState state) {
        if (state.is(GemstoneOreBlocks.RUBY_ORE)) return GemstoneItems.ROUGH_RUBY_GEMSTONE;
        if (state.is(GemstoneOreBlocks.AMETHYST_ORE)) return GemstoneItems.ROUGH_AMETHYST_GEMSTONE;
        if (state.is(GemstoneOreBlocks.AMBER_ORE)) return GemstoneItems.ROUGH_AMBER_GEMSTONE;
        if (state.is(GemstoneOreBlocks.TOPAZ_ORE)) return GemstoneItems.ROUGH_TOPAZ_GEMSTONE;
        if (state.is(GemstoneOreBlocks.SAPPHIRE_ORE)) return GemstoneItems.ROUGH_SAPPHIRE_GEMSTONE;
        if (state.is(GemstoneOreBlocks.JADE_ORE)) return GemstoneItems.ROUGH_JADE_GEMSTONE;
        if (state.is(GemstoneOreBlocks.OPAL_ORE)) return GemstoneItems.ROUGH_OPAL_GEMSTONE;
        if (state.is(GemstoneOreBlocks.JASPER_ORE)) return GemstoneItems.ROUGH_JASPER_GEMSTONE;
        return null;
    }
}
