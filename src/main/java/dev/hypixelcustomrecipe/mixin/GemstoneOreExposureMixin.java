package dev.hypixelcustomrecipe.mixin;

import java.util.function.Function;
import dev.hypixelcustomrecipe.GemstoneOreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gemstones need three supported faces; Rainbow needs all six faces covered by End Stone. */
@Mixin(OreFeature.class)
public abstract class GemstoneOreExposureMixin {
    @Inject(method = "canPlaceOre", at = @At("HEAD"), cancellable = true)
    private static void hypixelcustomrecipe$requireSupportedFaces(
            BlockState state, Function<BlockPos, BlockState> adjacentStateAccessor,
            RandomSource random, OreConfiguration config, OreConfiguration.TargetBlockState targetState,
            BlockPos.MutableBlockPos mutablePos, CallbackInfoReturnable<Boolean> cir
    ) {
        if (targetState.state.is(GemstoneOreBlocks.RAINBOW_ORE)) {
            for (Direction direction : Direction.values()) {
                if (!adjacentStateAccessor.apply(mutablePos.relative(direction)).is(Blocks.END_STONE)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
            return;
        }
        if (!hypixelcustomrecipe$isGemstoneOre(targetState.state)) return;
        int supportedFaces = 0;
        for (Direction direction : Direction.values()) {
            BlockState neighbor = adjacentStateAccessor.apply(mutablePos.relative(direction));
            if (neighbor.is(Blocks.END_STONE) || hypixelcustomrecipe$isGemstoneOre(neighbor)) supportedFaces++;
        }
        if (supportedFaces < 3) cir.setReturnValue(false);
    }

    private static boolean hypixelcustomrecipe$isGemstoneOre(BlockState state) {
        return state.is(GemstoneOreBlocks.RUBY_ORE) || state.is(GemstoneOreBlocks.AMETHYST_ORE)
                || state.is(GemstoneOreBlocks.AMBER_ORE) || state.is(GemstoneOreBlocks.TOPAZ_ORE)
                || state.is(GemstoneOreBlocks.SAPPHIRE_ORE) || state.is(GemstoneOreBlocks.JADE_ORE)
                || state.is(GemstoneOreBlocks.OPAL_ORE) || state.is(GemstoneOreBlocks.JASPER_ORE);
    }
}
