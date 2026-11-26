package dev.hypixelcustomrecipe;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Hypixel-style gemstone heads sourced from the Minecraft-Heads Hypixel gemstone entries.
 * Names and registry IDs use the correct spelling, independent of source head display names.
 */
public final class GemstoneItems {
    private static final String NAMESPACE = "hypixel";

    public static final Item ROUGH_RUBY = head("rough_ruby_gemstone", "Rough Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDE1OWIwMzI0M2JlMThhMTRmM2VhZTc2M2M0NTY1Yzc4ZjFmMzM5YTg3NDJkMjZmZGU1NDFiZTU5YjdkZTA3In19fQ==");
    public static final Item FLAWED_RUBY = head("flawed_ruby_gemstone", "Flawed Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDZkODEwNjhjYmRmNGEzNjQyMzFhMjY0NTNkNmNkNjYwYTAwOTVmOWNkODc5NTMwN2M1YmU2Njc0Mjc3MTJlIn19fQ==");
    public static final Item FLAWLESS_RUBY = head("flawless_ruby_gemstone", "Flawless Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTI2YTI0OGZiYmMwNmNmMDZlMmM5MjBlY2ExY2FjOGEyYzk2MTY0ZDMyNjA0OTRiZWQxNDJkNTUzMDI2Y2M2In19fQ==");
    public static final Item PERFECT_AMBER = head("perfect_amber_gemstone", "Perfect Amber Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdhZTIzNmNkZWMzZjJhNmY1MWVhZTE1ZTJjOGY2MjI4YjM0ZjEzN2RhMTU2OWZlYzllODAzZjljZDgxNzU5ZCJ9fX0=");
    public static final Item FLAWLESS_SAPPHIRE = head("flawless_sapphire_gemstone", "Flawless Sapphire Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTU3Y2ZhOWM3NWJhNTg0NjQ1ZWUyYWY2ZDk4NjdkNzY3ZGRlYTQ2NjdjZGZjNzJkYzEwNjFkZDE5NzVjYTdkMCJ9fX0=");
    public static final Item FLAWLESS_JADE = head("flawless_jade_gemstone", "Flawless Jade Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjg5Zjc1ZTBiMDAzNzhhNTgzZGJiYTcyOGRjZGM2ZTkzNDZmMzFkZDYwMWQ0NDhmM2Q2MDYxNWM3NDY1Y2MzZSJ9fX0=");
    public static final Item FLAWED_JASPER = head("flawed_jasper_gemstone", "Flawed Jasper Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTczNTExZTUwNGMzMTZiMTM5ZWRiMzVmZWJlNzNlZjU5MWMwZjQ1NWU4Y2FmOWVlMzUzYmMxMmI2YzE0YTkyMiJ9fX0=");
    public static final Item FINE_JASPER = head("fine_jasper_gemstone", "Fine Jasper Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWFjMTVmNmZjZjJjZTk2M2VmNGNhNzFmMWE4Njg1YWRiOTdlYjc2OWUxZDExMTk0Y2JiZDJlOTY0YTg4OTc4YyJ9fX0=");
    public static final Item FLAWED_TOPAZ = head("flawed_topaz_gemstone", "Flawed Topaz Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjYzOTI3NzNkMTE0YmUzMGFlYjNjMDljOTBjYmU2OTFmZmVhY2ViMzk5YjUzMGZlNmZiNTNkZGMwY2VkMzcxNCJ9fX0=");
    public static final Item FINE_OPAL = head("fine_opal_gemstone", "Fine Opal Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ3OThlMjBhNDdkMjUxYTllMzNkNDAzMzI5NzNjNzE4OWFjMTU1MDc2MGJhMjVjNGI5NTZjOTE1OTM2NDU2OCJ9fX0=");
    public static final Item PERFECT_OPAL = head("perfect_opal_gemstone", "Perfect Opal Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2U1MDlmMDZiOGRjMzg0MzU4ZmYyNDcyYWI2MmNlNmZkYzJmNjQ2ZTMzOGVmZGMzYzlmYjA1ZGRjNDMxZjY0In19fQ==");

    private GemstoneItems() {}

    private static Item head(String path, String displayName, String textureValue) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Property texture = new Property("textures", textureValue);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("hypixel:" + path).getBytes(StandardCharsets.UTF_8)),
                "HypixelGemstone", properties);
        Item.Properties itemProperties = new Item.Properties()
                .setId(key)
                .stacksTo(64)
                .component(DataComponents.CUSTOM_NAME, Component.literal(displayName))
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .component(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        Item item = new SimplePolymerItem(itemProperties, Items.PLAYER_HEAD) {
            @Override
            public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack,
                                                   PacketContext context, HolderLookup.Provider lookup) {
                ResolvableProfile itemProfile = stack.get(DataComponents.PROFILE);
                if (itemProfile != null) out.set(DataComponents.PROFILE, itemProfile);
            }
        };
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // Referencing this class initializes and registers the gemstone items.
    }
}
