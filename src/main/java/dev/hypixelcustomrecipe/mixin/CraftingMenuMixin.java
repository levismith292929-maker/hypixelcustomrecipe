package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.BulkDiamondCrafting;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private ResultContainer resultSlots;

    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$showBulkDiamondResult(Container container, CallbackInfo ci) {
        if (BulkDiamondCrafting.shouldHandle(this.craftSlots)) {
            this.resultSlots.setItem(0, BulkDiamondCrafting.getResult(this.craftSlots));
            ci.cancel();
        }
    }
}
