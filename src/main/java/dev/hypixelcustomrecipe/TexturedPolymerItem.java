package dev.hypixelcustomrecipe;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A Polymer item that explicitly keeps its custom item model when Polymer
 * creates the client-side representation of the stack.
 */
public final class TexturedPolymerItem extends SimplePolymerItem {
    private final Identifier itemModel;

    public TexturedPolymerItem(Item.Properties properties, Item fallback, Identifier itemModel) {
        super(properties, fallback);
        this.itemModel = itemModel;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return this.itemModel;
    }
}
