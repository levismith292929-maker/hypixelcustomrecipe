package dev.hypixelcustomrecipe;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.List;

/**
 * Real registered item IDs for items that were previously represented by
 * vanilla items plus custom_data in the old datapack.
 */
public final class DatapackItems {
    private static final String NAMESPACE = "hypixel";

    public static final Item AOTV = register("aotv", "Aspect of the Void", Items.DIAMOND_SWORD,
            "Ability: Instant Transmission  RIGHT CLICK",
            "Hold it in your main hand to see where you are aiming.",
            "Right-click to teleport onto that block (up to 64 blocks away).",
            "Mana Cost: 50",
            "Cooldown: 3 ticks");
    public static final Item AOTD = register("aotd", "Aspect of the Dragon", Items.NETHERITE_SWORD,
            "Ability: Dragon Rage  RIGHT CLICK",
            "Breathe fire 5 blocks in the direction you look.",
            "Each mob is hit once for 18 base damage, increased by Strength and Intelligence.",
            "Mana Cost: 80",
            "Cooldown: 12 ticks");
    public static final Item ASTRAEA = registerColored("astraea", "Astraea", Items.NETHERITE_SWORD, 0xFF55FF,
            "Ability: Wither Shield  RIGHT CLICK",
            "Gain Resistance II and Absorption IV for 10 seconds,",
            "with wither orbs spinning around you.",
            "Mana Cost: 40",
            "Cooldown: 10 seconds");
    public static final Item HYPERION = registerColored("hyperion", "Hyperion", Items.NETHERITE_SWORD, 0xFFAA00,
            "Ability: Implosion  RIGHT CLICK",
            "Deal 60 base ability damage to every target within 5 blocks.",
            "Damage scales with Strength and Intelligence.",
            "Mana Cost: 150",
            "Cooldown: 5 seconds");
    public static final Item NECRONS_SCYTHE = register("necrons_scythe", "Necron's Scythe", Items.DIAMOND_HOE,
            "Ability: Wither Impact  RIGHT CLICK",
            "Slash forward with a sweeping strike, damaging nearby enemies.",
            "Mana Cost: 35",
            "Cooldown: 15 ticks");
    public static final Item ZOMBIE_SWORD = register("zombie_sword", "Zombie Sword", Items.GOLDEN_SWORD,
            "Ability: Instant Healing  RIGHT CLICK",
            "Instantly heals 1 heart.",
            "Mana Cost: 50",
            "Cooldown: 1 second");
    public static final Item ORE_TRACKER = register("ore_tracker", "Ore Tracker", Items.COMPASS,
            "Ability: Ore Search  RIGHT CLICK",
            "Find the nearest diamond ore or ancient debris",
            "within 32 blocks in your current dimension.",
            "Mana Cost: 100",
            "Cooldown: 30 seconds");
    public static final Item STAT_VIEWER = register("stat_viewer", "View Stats", Items.NETHER_STAR,
            "Right-click to view your current SkyBlock stats.");

    public static final Item PET_ENDERMAN = registerProfileHead("pet_enderman", "Enderman", "MHF_Enderman", null,
            "Ability: Summon Pet", "Max Mana: 1000", "Mana Regeneration: 50/sec", "Damage: -20%");
    public static final Item PET_ENDER_DRAGON = registerProfileHead("pet_ender_dragon", "Ender Dragon", null,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWVjM2ZmNTYzMjkwYjEzZmYzYmNjMzY4OThhZjdlYWE5ODhiNmNjMThkYzI1NDE0N2Y1ODM3NGFmZTliMjFiOSJ9fX0=",
            "Ability: Summon Pet", "Max Mana: 300", "Mana Regeneration: 20/sec", "Damage: +15%");
    public static final Item PET_GOLDEN_DRAGON = registerProfileHead("pet_golden_dragon", "Golden Dragon", null,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmU5ZjliMWZjMDE0MTY2Y2I0NmEwOTNlNTM0OWIyYmY2ZWRkMjAxYjY4MGQ2MmU0OGRiZjNhZjliMDQ1OTExNiJ9fX0=",
            "Ability: Summon Pet", "Damage: +40%", "Max Mana: 750", "Mana Regeneration: 10/sec");
    public static final Item PET_IRON_GOLEM = registerProfileHead("pet_iron_golem", "Iron Golem", "MHF_Golem", null,
            "Ability: Summon Pet", "Damage: +5%", "Max Mana: 600", "Mana Regeneration: 30/sec", "Permanent Resistance I");
    public static final Item PET_REMOVER = registerProfileHead("pet_remover", "Pet Remover", "MHF_TNT2", null,
            "Ability: Dismiss Pet", "Removes your active pet and restores your normal mana stats.");

    private DatapackItems() {}

    private static Item register(String path, String name, Item fallback, String... loreLines) {
        return register(path, name, fallback, null, loreLines);
    }

    private static Item registerColored(String path, String name, Item fallback,
                                        Integer color, String... loreLines) {
        return register(path, name, fallback, color, loreLines);
    }

    private static Item register(String path, String name, Item fallback,
                                 Integer color, String... loreLines) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Component displayName = color != null
                ? ItemFormatting.name(name, color)
                : ItemFormatting.name(name, ItemFormatting.rarityColor(path));
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.CUSTOM_NAME, displayName)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (loreLines.length > 0) {
            List<Component> lore = Arrays.stream(loreLines)
                    .<Component>map(ItemFormatting::lore)
                    .toList();
            properties.component(DataComponents.LORE, new ItemLore(lore));
        }
        Item item = new eu.pb4.polymer.core.api.item.SimplePolymerItem(properties, fallback);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }


    private static Item registerProfileHead(String path, String name, String profileName,
                                             String textureValue, String... loreLines) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        ResolvableProfile profile;
        if (textureValue != null) {
            Property texture = new Property("textures", textureValue);
            PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
            GameProfile gameProfile = new GameProfile(
                    UUID.nameUUIDFromBytes(("hypixel:" + path).getBytes(StandardCharsets.UTF_8)),
                    "HypixelPet", properties);
            profile = ResolvableProfile.createResolved(gameProfile);
        } else {
            profile = ResolvableProfile.createUnresolved(profileName);
        }

        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.CUSTOM_NAME, ItemFormatting.name(name, ItemFormatting.rarityColor(path)))
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .component(DataComponents.PROFILE, profile);
        if (loreLines.length > 0) {
            List<Component> lore = Arrays.stream(loreLines).<Component>map(ItemFormatting::lore).toList();
            properties.component(DataComponents.LORE, new ItemLore(lore));
        }
        Item item = new SimplePolymerItem(properties, Items.PLAYER_HEAD) {
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
        // Referencing this class initializes and registers every field above.
    }
}
