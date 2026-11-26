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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Gemstone masks, Gem Tank, and Nature Gem accessory items. */
public final class GemstoneAccessoryItems {
    private static final String NS = "hypixel";

    public static final Item RUBY_MASK = head("ruby_mask", "Ruby Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTMwNzdiNTM2OTcwNGU3NmE4ODU5NWRiNzU0Y2QzOWYxMzQ0YTg2MzhiOTZhYzRlYWEyNmM1ZGNjMjA4MWVkMiJ9fX0=");
    public static final Item AMETHYST_MASK = head("amethyst_mask", "Amethyst Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTc1YjVhMjRlNTNjZjU2YmFhYTFjODkxNDRjMjdkMjg3NjVlNWQwODExNjNiZmNiMmJlYzJjNWY4ZWEzNmY1NiJ9fX0=");
    public static final Item AMBER_MASK = head("amber_mask", "Amber Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjE5MTcwOWU3ZDVkOTdkZTQyZjBmNzNjMjgxM2JhM2JlNWJlMWI0NTE0ZjUzYTFlMDIyMzI5NzdlMDUyNmY0YSJ9fX0=");
    public static final Item TOPAZ_MASK = head("topaz_mask", "Topaz Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTZkYmE4ZmQ2ZDFkNTc4NDU1MTVjYWI4NGIzMDM3YTliZDJiYjFkYjNhYzAxMjg4Nzg2ODI4NzY1ZGRlMTY3ZiJ9fX0=");
    public static final Item SAPPHIRE_MASK = head("sapphire_mask", "Sapphire Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODEzNGI4OTdhMDRiNTRiZjAwNzgxZjk3YzI1OWQxY2YyOWFkNDZjZDk5YzMxYjA1OGFlMDZkZDQ1ZGNhNWExYSJ9fX0=");
    public static final Item JADE_MASK = head("jade_mask", "Jade Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTA0YWMwM2Q1MWExOTU3N2UwN2Q3NTg4OTM3NjI3ZWQ0MjUzZTcwZTY1Y2MzMGNjOGNhODkyY2M4NjBmYzBlZiJ9fX0=");
    public static final Item OPAL_MASK = head("opal_mask", "Opal Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2M4NjRjMDhkODU3NmNjMWQ3MzQ0MmU4MGFiODNkMTM3NWI2OTI4ZmIwYzQ4MmNmZDM0NzVhM2JiZGZhZDhmNCJ9fX0=");
    public static final Item JASPER_MASK = head("jasper_mask", "Jasper Mask",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmY5NTg1NWFhNWRhZjYyMTBhZjFiZGI2MWJjNzM5YjIyZDYwOThjMmI3ZTAwZTVmZTEwNmU4M2NmNmQwNzQzNSJ9fX0=");

    public static final Item GEM_TANK = register("gem_tank", "Gem Tank",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA5ZmY4OTEzOGJhMWRiNDBlYjcyMzU5Y2Q5YmVjYWVjNTgxYzM3NGRkZGJlYTc0ZjE0M2Y2ZTg3NWVhNGU5NiJ9fX0=", true);
    public static final Item NATURE_GEM = head("nature_gem", "Nature Gem",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjAzN2VmNzE4NDY5OTRjZDk0MGFkOWIxNzcyZWE0ZWJmZjA2NjAwYWI4OWFhMzg2YTkxM2FlYjQ3NTQ4N2JlYiJ9fX0=");

    private GemstoneAccessoryItems() {}

    private static Item head(String path, String name, String texture) {
        return register(path, name, texture, false);
    }

    private static Item register(String path, String name, String texture, boolean tank) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(NS, path));
        Property property = new Property("textures", texture);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", property));
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("hypixel:" + path).getBytes(StandardCharsets.UTF_8)),
                "HypixelAccessory", properties);
        Item.Properties itemProperties = new Item.Properties()
                .setId(key).stacksTo(1)
                .component(DataComponents.CUSTOM_NAME, Component.literal(name))
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .component(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        Item item = new SimplePolymerItem(itemProperties, Items.PLAYER_HEAD) {
            @Override
            public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack,
                                                   PacketContext context, HolderLookup.Provider lookup) {
                ResolvableProfile itemProfile = stack.get(DataComponents.PROFILE);
                if (itemProfile != null) out.set(DataComponents.PROFILE, itemProfile);
            }

            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (tank && hand == InteractionHand.OFF_HAND && !level.isClientSide()
                        && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    GemTankMenu.open(serverPlayer);
                    return InteractionResultHolder.success(stack);
                }
                return super.use(level, player, hand);
            }
        };
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // Static fields register all ten accessories.
    }
}
