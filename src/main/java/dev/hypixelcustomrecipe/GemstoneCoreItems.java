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

/** The eight Hypixel gemstone cores, using the supplied Minecraft-Heads textures. */
public final class GemstoneCoreItems {
    private static final String NAMESPACE = "hypixel";

    public static final Item RUBY_GEMSTONE_CORE = head("ruby_gemstone_core", "Ruby Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTRlYmY1NjA2YWM4M2E3NGMxY2I5ZjdlNTYwNGJlM2NlMjk3ODkyYTRjYTEwMDVjZTUxMGNhMjVlYmEyODg4In19fQ==");
    public static final Item AMETHYST_GEMSTONE_CORE = head("amethyst_gemstone_core", "Amethyst Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTM2NWYzNzBhMTJkMDU4YzFmZmJlZTkwMmNlNjQwMTRhMmI4NThlNDA0ZmYzMDJlNjJmYjNkYmFmZWYzMjAwNCJ9fX0=");
    public static final Item AMBER_GEMSTONE_CORE = head("amber_gemstone_core", "Amber Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTUwOWMyMzZjZjg5OGZlNjJjN2I1NDQ2ODE4MzM4MTYwY2I5NTU2MzQ4NmQzZTI5YWZmZmRmODg3N2MwOGJmNCJ9fX0=");
    public static final Item TOPAZ_GEMSTONE_CORE = head("topaz_gemstone_core", "Topaz Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzcxNTA5NTc0MzQ0MjQxYjJjZGZmZmJiZjFkYzdhZjgzNDQwNTUzMzU0ZDBhNWM1MzhlZjllZWQ2NTYzNjE5OSJ9fX0=");
    public static final Item SAPPHIRE_GEMSTONE_CORE = head("sapphire_gemstone_core", "Sapphire Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQ1ZDdmZjFkNjJkZTU5MTMxMGZlZTE2MzAyNDZjZGE0OWEzYzI4Y2FiMDYzYmZmNTQ3MDNiNWQ5NWUyNzFkMCJ9fX0=");
    public static final Item JADE_GEMSTONE_CORE = head("jade_gemstone_core", "Jade Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTBkZDIwZDI5ZDc3NmI3OWU0ODUyMDBhYmVkY2M2ZDk0YzExYmQ0Y2E1YjE0OWM2MGE0MDQxYzQ2NjhiYjZhOCJ9fX0=");
    public static final Item OPAL_GEMSTONE_CORE = head("opal_gemstone_core", "Opal Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQ5ZDJiNWEwNjQxZjNhN2Q3YWFhYmNhYmYyOTRkNmVlMGY5ZDY2MjE2M2Y1MTYyNmFiODIzNTA5NGMyM2NlYyJ9fX0=");
    public static final Item JASPER_GEMSTONE_CORE = head("jasper_gemstone_core", "Jasper Gemstone Core",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDUwMjNhY2YyNTIzMzNjZWZjNjY4ZDg5YzEwYzE2MDA4NDc5NjBjMWNmN2QzZTdiYzE3NTAzNWU4MzcxY2ExNSJ9fX0=");

    private GemstoneCoreItems() {}

    private static Item head(String path, String displayName, String textureValue) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Property texture = new Property("textures", textureValue);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("hypixel:" + path).getBytes(StandardCharsets.UTF_8)),
                "HypixelGemstone", properties);
        Component name = Component.literal(displayName);
        Item.Properties itemProperties = new Item.Properties()
                .setId(key)
                .stacksTo(64)
                .component(DataComponents.CUSTOM_NAME, name)
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
        // Referencing this class initializes and registers all eight core items.
    }
}
