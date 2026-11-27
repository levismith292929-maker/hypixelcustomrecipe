package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.PlayerPlacedBlockData;
import dev.hypixelcustomrecipe.NecronEncounter;
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
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$restrictNecronArenaBuilding(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (context.getPlayer() instanceof ServerPlayer player
                && context.getLevel() instanceof ServerLevel
                && !NecronEncounter.canPlaceBlock(player, context.getClickedPos())) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Building is disabled during Necron's Guardian laser!"));
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

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
