package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.PlayerPlacedBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries the placement marker with a block when a piston finishes moving it. */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntityMixin {
    @Shadow @Final private boolean isSourcePiston;

    @Inject(method = "finalTick", at = @At("HEAD"))
    private void hypixelcustomrecipe$movePlacementMarker(CallbackInfo ci) {
        if (isSourcePiston) return;

        PistonMovingBlockEntity movingBlock = (PistonMovingBlockEntity) (Object) this;
        if (!(movingBlock.getLevel() instanceof ServerLevel level)) return;

        BlockPos destination = movingBlock.getBlockPos();
        BlockPos source = destination.relative(movingBlock.getPushDirection().getOpposite());
        PlayerPlacedBlockData.get(level).move(level, source, destination);
    }
}
