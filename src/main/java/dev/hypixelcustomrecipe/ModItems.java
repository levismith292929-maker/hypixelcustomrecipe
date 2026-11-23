package dev.hypixelcustomrecipe;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.itemgroup.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ModItems {
    private static final String ITEM_NAMESPACE = "hypixel";

    public static final Item OLD_DRAGON_FRAGMENT = register("old_dragon_fragment", "Old Dragon Fragment", Items.COPPER_INGOT);
    public static final Item PROTECTOR_DRAGON_FRAGMENT = register("protector_dragon_fragment", "Protector Dragon Fragment", Items.DIAMOND);
    public static final Item WISE_DRAGON_FRAGMENT = register("wise_dragon_fragment", "Wise Dragon Fragment", Items.LAPIS_LAZULI);
    public static final Item YOUNG_DRAGON_FRAGMENT = register("young_dragon_fragment", "Young Dragon Fragment", Items.PRISMARINE_SHARD);
    public static final Item STRONG_DRAGON_FRAGMENT = register("strong_dragon_fragment", "Strong Dragon Fragment", Items.OBSIDIAN);
    public static final Item UNSTABLE_DRAGON_FRAGMENT = register("unstable_dragon_fragment", "Unstable Dragon Fragment", Items.REDSTONE_BLOCK);
    public static final Item SUPERIOR_DRAGON_FRAGMENT = register("superior_dragon_fragment", "Superior Dragon Fragment", Items.DIAMOND_BLOCK);
    public static final Item HOLY_DRAGON_FRAGMENT = register("holy_dragon_fragment", "Holy Dragon Fragment", Items.EMERALD);
    public static final Item NECRON_CORE = register("necron_core", "Necron Core", Items.NETHER_STAR);

    private ModItems() {
    }

    private static Item register(String path, String displayName, Item vanillaFallback) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(ITEM_NAMESPACE, path)
        );
        Item item = new SimplePolymerItem(
                new Item.Properties()
                        .setId(key)
                        .stacksTo(64)
                        .component(DataComponents.CUSTOM_NAME, Component.literal(displayName)),
                vanillaFallback
        );
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(OLD_DRAGON_FRAGMENT);
            entries.accept(PROTECTOR_DRAGON_FRAGMENT);
            entries.accept(WISE_DRAGON_FRAGMENT);
            entries.accept(YOUNG_DRAGON_FRAGMENT);
            entries.accept(STRONG_DRAGON_FRAGMENT);
            entries.accept(UNSTABLE_DRAGON_FRAGMENT);
            entries.accept(SUPERIOR_DRAGON_FRAGMENT);
            entries.accept(HOLY_DRAGON_FRAGMENT);
            entries.accept(NECRON_CORE);
        });
    }
}
