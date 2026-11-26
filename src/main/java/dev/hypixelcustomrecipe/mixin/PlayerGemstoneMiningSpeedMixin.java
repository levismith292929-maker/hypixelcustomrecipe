package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.GemstoneOreMiningHandler;
import dev.hypixelcustomrecipe.DatapackItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the configured tier-based mining times to gemstone ore blocks. */
@Mixin(Player.class)
public abstract class PlayerGemstoneMiningSpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void hypixelcustomrecipe$applyGemstoneMiningSpeed(
            BlockState state, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        ItemStack tool = player.getMainHandItem();
        if (tool.getItem() == DatapackItems.ADMIN_DRILL) {
            cir.setReturnValue(1_000_000.0F);
            return;
        }
        float gemstoneSpeed = GemstoneOreMiningHandler.getGemstoneMiningSpeed(state, tool);
        if (gemstoneSpeed >= 0.0F) {
            cir.setReturnValue(gemstoneSpeed);
        }
    }
}
