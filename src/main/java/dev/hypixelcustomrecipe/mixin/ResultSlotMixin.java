package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.BulkDiamondCrafting;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow @Final private Container craftSlots;

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$consumeBulkDiamondIngredients(Player player, ItemStack stack, CallbackInfo ci) {
        if (BulkDiamondCrafting.consume(this.craftSlots, stack.getItem())) {
            ci.cancel();
        }
    }
}
