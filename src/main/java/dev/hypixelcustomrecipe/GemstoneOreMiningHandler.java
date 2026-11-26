package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

public final class GemstoneOreMiningHandler {
    private static final double[] MINIMUM_TOOL_SECONDS =
            {2.0, 3.0, 5.0, 6.0, 8.0, 9.0, 10.0, 12.0};
    private static final double[] NEXT_TOOL_SECONDS =
            {1.5, 2.5, 4.5, 5.5, 7.5, 8.5, 9.5, 11.0};
    private static final double[] TWO_TIERS_ABOVE_SECONDS =
            {1.0, 2.0, 4.0, 5.0, 7.0, 8.0, 9.0, 10.0};
    private static final double[] EXTRA_TIER_REDUCTION =
            {0.25, 0.25, 0.25, 0.5, 0.5, 0.75, 0.75, 1.0};

    private static final int GEMSTONE_GAUNTLET_TIER = 9;
    private static final float GEMSTONE_ORE_HARDNESS = 3.0F;

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

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(
                        Component.literal("You need a stronger mining tool to mine this gemstone.")
                );
            }
            return false;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            int oreTier = getOreTier(state);
            if (oreTier < 0 || player.getAbilities().instabuild
                    || !(level instanceof ServerLevel serverLevel)
                    || !(player instanceof ServerPlayer serverPlayer)) {
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

            // Gemstone Fortune is an additional chance to roll one step higher.
            // It never exceeds the Fortune III maximum of eight gemstones.
            double gemstoneFortune = AccessoryBag.gemstoneFortuneBonusPercent(serverPlayer);
            if (amount < 8 && level.getRandom().nextDouble() * 100.0 < gemstoneFortune) {
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
     * Returns the custom destroy speed for a gemstone ore, or -1 when this
     * item is not allowed to mine that ore. The times are calibrated against
     * the ore's 3.0 hardness and Minecraft's normal 20 ticks-per-second rate.
     */
    public static float getGemstoneMiningSpeed(BlockState state, ItemStack tool) {
        int oreTier = getOreTier(state);
        int toolTier = getToolTier(tool);
        if (oreTier < 0 || toolTier < oreTier) {
            return -1.0F;
        }

        // The gauntlet is the final tier; higher tiers never reduce time further.
        toolTier = Math.min(toolTier, GEMSTONE_GAUNTLET_TIER);

        double seconds;
        if (toolTier == oreTier) {
            seconds = MINIMUM_TOOL_SECONDS[oreTier];
        } else if (toolTier == oreTier + 1) {
            seconds = NEXT_TOOL_SECONDS[oreTier];
        } else {
            int extraTiers = Math.max(0, toolTier - (oreTier + 2));
            seconds = TWO_TIERS_ABOVE_SECONDS[oreTier]
                    - EXTRA_TIER_REDUCTION[oreTier] * extraTiers;
        }

        // A zero-second target means the ore should break on the first mining tick.
        if (seconds <= 0.0) {
            return 1_000_000.0F;
        }

        // Vanilla block progress per tick is destroySpeed / hardness / 30.
        // Convert the requested wall-clock time into the corresponding speed.
        return (GEMSTONE_ORE_HARDNESS * 1.5F) / (float) seconds;
    }

    /**
     * Mining tiers: Netherite Pickaxe = 0, Ruby through Jasper Drills = 1-8,
     * and Gemstone Gauntlet = 9. Ruby ore requires a Netherite Pickaxe;
     * each later ore requires the previous drill or better.
     */
    public static int getToolTier(ItemStack stack) {
        Item item = stack.getItem();

        // Polymer presents custom drills using a vanilla fallback item to clients.
        // Their ITEM_MODEL component keeps the real drill identity, so use it
        // before checking the server-side registered item instance.
        net.minecraft.resources.Identifier model = stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL);
        if (model != null && model.getNamespace().equals("hypixel")) {
            return switch (model.getPath()) {
                case "ruby_drill" -> 1;
                case "amethyst_drill" -> 2;
                case "amber_drill" -> 3;
                case "topaz_drill" -> 4;
                case "sapphire_drill" -> 5;
                case "jade_drill" -> 6;
                case "opal_drill" -> 7;
                case "jasper_drill" -> 8;
                case "gemstone_gauntlet" -> GEMSTONE_GAUNTLET_TIER;
                default -> item == Items.NETHERITE_PICKAXE ? 0 : -1;
            };
        }

        if (item == Items.NETHERITE_PICKAXE) return 0;
        if (item == DatapackItems.RUBY_DRILL) return 1;
        if (item == DatapackItems.AMETHYST_DRILL) return 2;
        if (item == DatapackItems.AMBER_DRILL) return 3;
        if (item == DatapackItems.TOPAZ_DRILL) return 4;
        if (item == DatapackItems.SAPPHIRE_DRILL) return 5;
        if (item == DatapackItems.JADE_DRILL) return 6;
        if (item == DatapackItems.OPAL_DRILL) return 7;
        if (item == DatapackItems.JASPER_DRILL) return 8;
        if (item == DatapackItems.GEMSTONE_GAUNTLET) return GEMSTONE_GAUNTLET_TIER;
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
