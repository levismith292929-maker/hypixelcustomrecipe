package dev.hypixelcustomrecipe;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.UUID;

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
    public static final Item NECRON_CORE = registerNecronCore();

    // Unique item ID so the AOTV recipe can require an AOTE,
    // rather than accepting any ordinary diamond sword.
    public static final Item AOTE = registerUnstackable("aote", "Aspect of the End", Items.DIAMOND_SWORD);

    private ModItems() {
    }

    private static Item registerNecronCore() {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(ITEM_NAMESPACE, "necron_core")
        );
        GameProfile profile = new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000001"), "NecronCore");
        profile.properties().put("textures", new Property("textures",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2UwZGNmNmRkYTg1Yzk1ZDdhOTQ3NmY1MTQ4ZGIxOTVkODliN2MyYjQxZTJlYzgwYWRjODIyYzZhOWNiY2UxNyJ9fX0="
        ));
        Item item = new SimplePolymerItem(
                new Item.Properties()
                        .setId(key)
                        .stacksTo(64)
                        .component(DataComponents.CUSTOM_NAME, Component.literal("Necron Core"))
                        .component(DataComponents.PROFILE, ResolvableProfile.createResolved(profile)),
                Items.PLAYER_HEAD
        );
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static Item register(String path, String displayName, Item vanillaFallback) {
        return register(path, displayName, vanillaFallback, 64);
    }

    private static Item registerUnstackable(String path, String displayName, Item vanillaFallback) {
        return register(path, displayName, vanillaFallback, 1);
    }

    private static Item register(String path, String displayName, Item vanillaFallback, int maxStackSize) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(ITEM_NAMESPACE, path)
        );
        Item item = new SimplePolymerItem(
                new Item.Properties()
                        .setId(key)
                        .stacksTo(maxStackSize)
                        .component(DataComponents.CUSTOM_NAME, Component.literal(displayName)),
                vanillaFallback
        );
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // Referencing this class initializes and registers all item fields.
    }
}
