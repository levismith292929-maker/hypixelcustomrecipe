package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.PlayerPlacedBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clears a placement marker whenever an explosion destroys a block. */
@Mixin(Block.class)
public abstract class ExplosionMixin {
    @Inject(method = "wasExploded", at = @At("HEAD"))
    private void hypixelcustomrecipe$clearDestroyedPlacementMarker(
            ServerLevel level,
            BlockPos pos,
            Explosion explosion,
            CallbackInfo ci
    ) {
        PlayerPlacedBlockData.get(level).remove(level, pos);
    }
}
