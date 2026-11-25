package dev.hypixelcustomrecipe.mixin;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses the exact crafting grid consumed by this result slot. */
@Mixin(ResultSlot.class)
public interface ResultSlotAccessor {
    @Accessor("craftSlots")
    CraftingContainer hypixelcustomrecipe$getCraftSlots();
}
