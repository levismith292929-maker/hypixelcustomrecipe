package dev.hypixelcustomrecipe;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hypixel-style bulk crafting: five matching stacks are accepted in any
 * crafting-table slots, with the specified amount consumed from each stack.
 */
public final class BulkDiamondCrafting {
    private static final int REQUIRED_SLOTS = 5;

    private BulkDiamondCrafting() {
    }

    public static ItemStack getResult(Container input) {
        if (matches(input, net.minecraft.world.item.Items.DIAMOND, 32)) {
            return new ItemStack(DatapackItems.ENCHANTED_DIAMOND, 1);
        }
        if (matches(input, DatapackItems.ENCHANTED_DIAMOND, 16)) {
            return new ItemStack(DatapackItems.ENCHANTED_DIAMOND_BLOCK, 1);
        }
        return ItemStack.EMPTY;
    }

    public static boolean consume(Container input, Item output) {
        Item ingredient;
        int amount;
        if (output == DatapackItems.ENCHANTED_DIAMOND) {
            ingredient = net.minecraft.world.item.Items.DIAMOND;
            amount = 32;
        } else if (output == DatapackItems.ENCHANTED_DIAMOND_BLOCK) {
            ingredient = DatapackItems.ENCHANTED_DIAMOND;
            amount = 16;
        } else {
            return false;
        }

        if (!matches(input, ingredient, amount)) {
            return false;
        }

        int consumedSlots = 0;
        for (int slot = 0; slot < input.getContainerSize() && consumedSlots < REQUIRED_SLOTS; slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && stack.is(ingredient)) {
                stack.shrink(amount);
                input.setItem(slot, stack);
                consumedSlots++;
            }
        }
        input.setChanged();
        return consumedSlots == REQUIRED_SLOTS;
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
