package dev.hypixelcustomrecipe;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hypixel-style bulk crafting: five matching stacks are accepted in any
 * crafting-table slots, with the specified amount consumed from each stack.
 */
public final class BulkDiamondCrafting {
    private static final int REQUIRED_SLOTS = 5;

    private BulkDiamondCrafting() {
    }

    public static ItemStack getResult(Container input) {
        if (matches(input, Items.DIAMOND, 32)) {
            return new ItemStack(DatapackItems.ENCHANTED_DIAMOND, 1);
        }
        if (matches(input, DatapackItems.ENCHANTED_DIAMOND, 16)) {
            return new ItemStack(DatapackItems.ENCHANTED_DIAMOND_BLOCK, 1);
        }
        return ItemStack.EMPTY;
    }

    /**
     * True when the grid contains exactly five stacks of one of the two
     * recipe ingredients. This also catches under-filled stacks so the
     * placeholder recipe-book recipe cannot be crafted with only one item.
     */
    public static boolean shouldHandle(Container input) {
        return hasExactlyFiveStacks(input, Items.DIAMOND)
                || hasExactlyFiveStacks(input, DatapackItems.ENCHANTED_DIAMOND);
    }

    public static boolean isBulkOutput(Item output) {
        return output == DatapackItems.ENCHANTED_DIAMOND
                || output == DatapackItems.ENCHANTED_DIAMOND_BLOCK;
    }

    /**
     * Vanilla's ResultSlot handling consumes one item from each ingredient
     * stack after this HEAD injection runs. Remove only the additional amount
     * here, allowing vanilla to consume the final item and run its normal
     * crafting hooks and result handling.
     */
    public static void consumeExtra(Container input, Item output) {
        Item ingredient;
        int extraAmount;
        int requiredAmount;
        if (output == DatapackItems.ENCHANTED_DIAMOND) {
            ingredient = Items.DIAMOND;
            requiredAmount = 32;
            extraAmount = 31;
        } else if (output == DatapackItems.ENCHANTED_DIAMOND_BLOCK) {
            ingredient = DatapackItems.ENCHANTED_DIAMOND;
            requiredAmount = 16;
            extraAmount = 15;
        } else {
            return;
        }

        if (!matches(input, ingredient, requiredAmount)) {
            return;
        }

        int consumedSlots = 0;
        for (int slot = 0; slot < input.getContainerSize() && consumedSlots < REQUIRED_SLOTS; slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && stack.is(ingredient)) {
                stack.shrink(extraAmount);
                input.setItem(slot, stack);
                consumedSlots++;
            }
        }
        input.setChanged();
    }

    private static boolean hasExactlyFiveStacks(Container input, Item ingredient) {
        int matchingSlots = 0;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(ingredient)) {
                return false;
            }
            matchingSlots++;
        }
        return matchingSlots == REQUIRED_SLOTS;
    }

    private static boolean matches(Container input, Item ingredient, int amount) {
        int matchingSlots = 0;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(ingredient) || stack.getCount() < amount) {
                return false;
            }
            matchingSlots++;
        }
        return matchingSlots == REQUIRED_SLOTS;
    }
}
