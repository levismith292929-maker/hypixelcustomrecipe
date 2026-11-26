package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.PlayerPlacedBlockData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks successful block placements made by players. */
@Mixin(BlockItem.class)
public abstract class BlockItemPlacementMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void hypixelcustomrecipe$recordPlayerPlacement(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel level)) return;
        if (!(context.getPlayer() instanceof ServerPlayer)) return;

        PlayerPlacedBlockData.get(level).add(level, context.getClickedPos());
    }
}
