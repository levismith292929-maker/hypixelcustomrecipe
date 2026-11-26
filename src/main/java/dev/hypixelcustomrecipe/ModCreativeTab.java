package dev.hypixelcustomrecipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * A single creative tab containing every item registered by this mod.
 * The tab is populated from the completed item registry so dynamically
 * registered armor pieces and gemstone ore block-items are included too.
 */
public final class ModCreativeTab {
    private static final Identifier TAB_ID =
            Identifier.fromNamespaceAndPath(HypixelCustomRecipe.MOD_ID, "items");

    private ModCreativeTab() {
    }

    public static void initialize() {
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.literal("Hypixel"))
                .icon(() -> new ItemStack(GemstoneItems.ROUGH_RUBY_GEMSTONE))
                .displayItems((parameters, output) -> BuiltInRegistries.ITEM.forEach(item -> {
                    Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
                    if ("hypixel".equals(itemId.getNamespace())) {
                        output.accept(new ItemStack(item));
                    }
                }))
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_ID, tab);
    }
}
