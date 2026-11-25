package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.BulkDiamondCrafting;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$showBulkDiamondResult(Container container, CallbackInfo ci) {
        var menu = (dev.hypixelcustomrecipe.mixin.AbstractCraftingMenuAccessor) this;
        CraftingContainer craftSlots = menu.hypixelcustomrecipe$getCraftSlots();
        if (BulkDiamondCrafting.shouldHandle(craftSlots)) {
            ResultContainer resultSlots = menu.hypixelcustomrecipe$getResultSlots();
            resultSlots.setItem(0, BulkDiamondCrafting.getResult(craftSlots));
            ci.cancel();
        }
    }
}
