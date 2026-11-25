package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.BulkDiamondCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$consumeBulkDiamondIngredients(Player player, ItemStack stack, CallbackInfo ci) {
        if (BulkDiamondCrafting.isBulkOutput(stack.getItem())
                && player.containerMenu instanceof CraftingMenu menu) {
            CraftingContainer craftSlots =
                    ((AbstractCraftingMenuAccessor) menu).hypixelcustomrecipe$getCraftSlots();
            BulkDiamondCrafting.consume(craftSlots, stack.getItem());
            ci.cancel();
        }
    }
}
