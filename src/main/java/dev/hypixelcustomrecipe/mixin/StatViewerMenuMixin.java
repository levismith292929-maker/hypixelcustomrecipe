package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.DatapackItems;
import dev.hypixelcustomrecipe.SkyblockAbilityHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets players click the Stat Viewer in an inventory without equipping it. */
@Mixin(AbstractContainerMenu.class)
public abstract class StatViewerMenuMixin {
    @Shadow
    public abstract Slot getSlot(int slotId);

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$openStatViewer(
            int slotId,
            int button,
            ContainerInput clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || clickType != ContainerInput.PICKUP
                || (button != 0 && button != 1)
                || slotId < 0) {
            return;
        }

        Slot clickedSlot;
        try {
            clickedSlot = getSlot(slotId);
        } catch (IndexOutOfBoundsException ignored) {
            return;
        }

        if (clickedSlot != null && clickedSlot.hasItem()
                && clickedSlot.getItem().is(DatapackItems.STAT_VIEWER)) {
            SkyblockAbilityHandler.showStats(serverPlayer);
            // Treat this as a button, not a normal inventory pickup.
            ci.cancel();
        }
    }
}
