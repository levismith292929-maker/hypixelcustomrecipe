package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Awards Mining XP for successfully broken blocks. Player-placement filtering is intentionally deferred. */
public final class MiningExperienceHandler {
    private MiningExperienceHandler() {}

    public static void initialize() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || player.getAbilities().instabuild) return;
            int xp = experienceFor(state);
            if (xp > 0) SkyblockPlayerState.get(serverPlayer).addMiningExperience(serverPlayer, xp);
        });
    }

    private static int experienceFor(BlockState state) {
        if (state.is(GemstoneOreBlocks.RUBY_ORE)) return 100;
        if (state.is(GemstoneOreBlocks.AMETHYST_ORE)) return 175;
        if (state.is(GemstoneOreBlocks.AMBER_ORE)) return 307;
        if (state.is(GemstoneOreBlocks.TOPAZ_ORE)) return 538;
        if (state.is(GemstoneOreBlocks.SAPPHIRE_ORE)) return 942;
        if (state.is(GemstoneOreBlocks.JADE_ORE)) return 1649;
        if (state.is(GemstoneOreBlocks.OPAL_ORE)) return 2886;
        if (state.is(GemstoneOreBlocks.JASPER_ORE)) return 5051;

        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        boolean deepslate = id.startsWith("deepslate_");
        String ore = deepslate ? id.substring("deepslate_".length()) : id;
        return switch (ore) {
            case "coal_ore" -> deepslate ? 4 : 2;
            case "copper_ore" -> deepslate ? 9 : 3;
            case "redstone_ore", "lapis_ore" -> deepslate ? 7 : 4;
            case "iron_ore" -> deepslate ? 15 : 8;
            case "gold_ore" -> deepslate ? 28 : 10;
            case "diamond_ore" -> deepslate ? 45 : 80;
            case "emerald_ore" -> deepslate ? 200 : 60;
            case "nether_quartz_ore" -> 10;
            case "nether_gold_ore" -> 6;
            case "ancient_debris" -> 500;
            case "stone", "andesite", "diorite", "granite", "deepslate", "tuff",
                    "calcite", "dripstone_block", "blackstone" -> 1;
            default -> 0;
        };
    }
}
