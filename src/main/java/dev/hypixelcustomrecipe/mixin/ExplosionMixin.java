package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.PlayerPlacedBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clears placement markers for blocks actually included in an explosion's destruction list. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private Level level;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void hypixelcustomrecipe$clearDestroyedPlacementMarkers(
            boolean particles,
            CallbackInfo ci
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        PlayerPlacedBlockData data = PlayerPlacedBlockData.get(serverLevel);
        for (BlockPos pos : ((Explosion) (Object) this).getToBlow()) {
            data.remove(serverLevel, pos);
        }
    }
}
