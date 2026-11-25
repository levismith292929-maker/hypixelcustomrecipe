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
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class ModItems {
    private static final String ITEM_NAMESPACE = "hypixel";

    public static final Item OLD_DRAGON_FRAGMENT = registerTexturedFragment(
            "old_dragon_fragment", "Old Dragon Fragment",
            "7aa09ad177fbccc53fa316cc04bdd2c9366baed889df76c5a29defea8170def5"
    );
    public static final Item PROTECTOR_DRAGON_FRAGMENT = registerTexturedFragment(
            "protector_dragon_fragment", "Protector Dragon Fragment",
            "48de339af63a229c9238d027e47f53eeb56141a419f51b35c31ea1494b435dd3"
    );
    public static final Item WISE_DRAGON_FRAGMENT = registerTexturedFragment(
            "wise_dragon_fragment", "Wise Dragon Fragment",
            "1d7620b2e4934963bb12508310d05494c067dc33e008cecf2cd7b4549654fab3"
    );
    public static final Item YOUNG_DRAGON_FRAGMENT = registerTexturedFragment(
            "young_dragon_fragment", "Young Dragon Fragment",
            "4b5bd6b64e8bd6c58f5cd1e79a5502d4448bafc006d2fe0568f6a0d6b86d449e"
    );
    public static final Item STRONG_DRAGON_FRAGMENT = registerTexturedFragment(
            "strong_dragon_fragment", "Strong Dragon Fragment",
            "6ee32fbd4c7b03b869078aa1f493a390e6e13b461d613707eafb326dbcd2b4b5"
    );
    public static final Item UNSTABLE_DRAGON_FRAGMENT = registerTexturedFragment(
            "unstable_dragon_fragment", "Unstable Dragon Fragment",
            "98228c234c3903c512a5a0aa45260e7b567e0e20eefc7d561ccec97b295871af"
    );
    public static final Item SUPERIOR_DRAGON_FRAGMENT = registerTexturedFragment(
            "superior_dragon_fragment", "Superior Dragon Fragment",
            "6f89b150be9c4c5249f355f68ea0c4391300a9be1f260d750fc35a1817ad796e"
    );

    public static final Item VOID_FRAGMENT = registerTexturedHeadFromValue(
            "void_fragment", "Void Fragment",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjIwMWFlMWE4YTA0ZGY1MjY1NmY1ZTQ4MTNlMWZiY2Y5Nzg3N2RiYmZiYzQyNjhkMDQzMTZkNmY5Zjc1MyJ9fX0=", 64,
            "Dropped by Voidridden enemies."
    );

    public static final Item NECRON_CORE = registerNecronCore();

    // Star-upgrade materials. Each shard is used for its matching anvil upgrade.
    public static final Item STAR_SHARD_1 = registerTexturedHeadFromValue(
            "star_shard1", "Star Shard 1",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjEzNjcyNDc0NWM4YzlhYmM3NWNkZjIyMDVmMmNkMWQzM2U2YmZlZmE0MTVmOTk1YmU3NjkyMjkxMjE2OWVjOSJ9fX0=", 64,
            "Used to upgrade non-vanilla armor to 1 star in an anvil."
    );
    public static final Item STAR_SHARD_2 = registerTexturedHeadFromValue(
            "star_shard2", "Star Shard 2",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDcxYjk1NGU4OWFlZDE1NmY5MGZhZmNlNmRjNzc4OGFjNzZiNjQ2NmNlY2NhM2E0ZjRlYzFlNDYzZWI5MTRhMSJ9fX0=", 64,
            "Used to upgrade non-vanilla armor to 2 stars in an anvil."
    );
    public static final Item STAR_SHARD_3 = registerTexturedHeadFromValue(
            "star_shard3", "Star Shard 3",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFjODA0MjUyN2Y4MWM4ZTI5M2UyODEwMTEzNDg5ZjQzOTRjYzZlZmUxNWQxYWZhYzQzMTU3MWM3M2I2MmRjNCJ9fX0=", 64,
            "Used to upgrade non-vanilla armor to 3 stars in an anvil."
    );

    // Unique item ID so the AOTV recipe can require an AOTE,
    // rather than accepting any ordinary diamond sword.
    public static final Item AOTE = registerUnstackable("aote", "Aspect of the End", Items.DIAMOND_SWORD,
            "Ability: Instant Transmission  RIGHT CLICK",
            "Teleports you 5 blocks in the direction you are facing.",
            "Mana Cost: 15",
            "Cooldown: 2 ticks");

    private ModItems() {
    }

    private static Item registerNecronCore() {
        return registerTexturedHead(
                "necron_core",
                "Necron Core",
                "ce0dcf6dda85c95d7a9476f5148db195d89b7c2b41e2ec80adc822c6a9cbce17",
                64
        );
    }

    private static Item registerTexturedFragment(String path, String displayName, String textureHash) {
        return registerTexturedHead(path, displayName, textureHash, 64);
    }

    private static Item registerTexturedHead(String path, String displayName, String textureHash, int maxStackSize) {
        String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/"
                + textureHash + "\"}}}";
        String textureValue = Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));
        return registerTexturedHeadFromValue(path, displayName, textureValue, maxStackSize);
    }

    private static Item registerTexturedHeadFromValue(String path, String displayName, String textureValue, int maxStackSize) {
        return registerTexturedHeadFromValue(path, displayName, textureValue, maxStackSize, null);
    }

    private static boolean isStarShard(String path) {
        return path.equals("star_shard1") || path.equals("star_shard2") || path.equals("star_shard3");
    }

    private static Item registerTexturedHeadFromValue(String path, String displayName, String textureValue, int maxStackSize, String description) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(ITEM_NAMESPACE, path)
        );
        Property texture = new Property("textures", textureValue);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("hypixel:" + path).getBytes(StandardCharsets.UTF_8)),
                "Hypixel",
                properties
        );

        Item.Properties itemProperties = new Item.Properties()
                .setId(key)
                .stacksTo(maxStackSize)
                .component(DataComponents.CUSTOM_NAME, isStarShard(path)
                        ? Component.literal(displayName).withStyle(style -> style
                                .withColor(net.minecraft.ChatFormatting.GOLD)
                                .withBold(true)
                                .withUnderlined(true)
                                .withItalic(false))
                        : ItemFormatting.name(displayName, ItemFormatting.rarityColor(path)))
                .component(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        if (description != null) {
            itemProperties.component(DataComponents.LORE, new ItemLore(java.util.List.of(
                    ItemFormatting.lore(description))));
        }
        Item item = new SimplePolymerItem(itemProperties, Items.PLAYER_HEAD) {
            @Override
            public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
                ResolvableProfile skullProfile = stack.get(DataComponents.PROFILE);
                if (skullProfile != null) {
                    out.set(DataComponents.PROFILE, skullProfile);
                }
            }
        };

        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static ItemAttributeModifiers diamondSwordAttributes() {
        // Do not create a default ItemStack while the item registry is still binding components.
        // These are the vanilla diamond sword's main-hand modifiers.
        return ItemAttributeModifiers.EMPTY
                .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("minecraft", "base_attack_damage"),
                                6.0,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .withModifierAdded(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("minecraft", "base_attack_speed"),
                                -2.4,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                );
    }

    private static Item register(String path, String displayName, Item vanillaFallback) {
        return register(path, displayName, vanillaFallback, 64);
    }

    private static Item registerUnstackable(String path, String displayName, Item vanillaFallback,
                                             String... loreLines) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(ITEM_NAMESPACE, path)
        );
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.CUSTOM_NAME, ItemFormatting.name(displayName, ItemFormatting.rarityColor(path)))
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, diamondSwordAttributes())
                .component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(ITEM_NAMESPACE, path));
        if (loreLines.length > 0) {
            ItemLore lore = new ItemLore(java.util.Arrays.stream(loreLines)
                    .<Component>map(ItemFormatting::lore).toList());
            properties.component(DataComponents.LORE, lore);
        }
        Item item = new SimplePolymerItem(properties, vanillaFallback);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
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
