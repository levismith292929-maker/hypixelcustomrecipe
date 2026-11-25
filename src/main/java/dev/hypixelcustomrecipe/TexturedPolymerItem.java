package dev.hypixelcustomrecipe;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A Polymer item that keeps its custom item model on the client-side
 * representation created by Polymer.
 */
public final class TexturedPolymerItem extends SimplePolymerItem {
    private final Identifier itemModel;

    public TexturedPolymerItem(Item.Properties properties, Item fallback, Identifier itemModel) {
        super(properties, fallback);
        this.itemModel = itemModel;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context,
                                           HolderLookup.Provider lookup) {
        super.modifyBasePolymerItemStack(out, stack, context, lookup);
        out.set(DataComponents.ITEM_MODEL, this.itemModel);
    }
}
