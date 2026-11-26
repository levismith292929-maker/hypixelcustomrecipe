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
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
            "Damage: 120", "Strength: 100",
            "Ability: Instant Transmission  RIGHT CLICK",
            "Hold it in your main hand to see where you are aiming.",
            "Right-click to teleport onto that block (up to 64 blocks away).",
            "Mana Cost: 50",
            "Cooldown: 3 ticks");
    public static final Item AOTD = register("aotd", "Aspect of the Dragon", Items.NETHERITE_SWORD,
            "Damage: 225", "Strength: 100",
            "Ability: Dragon Rage  RIGHT CLICK",
            "Breathe fire 5 blocks in the direction you look.",
            "Each mob is hit once for 100 base ability damage, increased by Strength and Intelligence.",
            "Mana Cost: 80",
            "Cooldown: 12 ticks");
    public static final Item ASTRAEA = registerColored("astraea", "Astraea", Items.NETHERITE_SWORD, 0xFF55FF,
            "Damage: 270", "Strength: 150", "Defense: 250", "Intelligence: 50",
            "Ability: Wither Shield  RIGHT CLICK",
            "Gain Resistance II and Absorption IV for 10 seconds,",
            "with wither orbs spinning around you.",
            "Mana Cost: 40",
            "Cooldown: 10 seconds");
    public static final Item HYPERION = registerColored("hyperion", "Hyperion", Items.NETHERITE_SWORD, 0xFFAA00,
            "Damage: 260", "Strength: 150", "Intelligence: 350",
            "Ability: Implosion  RIGHT CLICK",
            "Deal 200 base ability damage to every target within 5 blocks.",
            "Damage scales with Strength and Intelligence.",
            "Mana Cost: 150",
            "Cooldown: 5 seconds");
    public static final Item VOIDSPLIT_KATANA = register("voidsplit_katana", "Voidsplit Katana", Items.NETHERITE_SWORD,
            "A blade forged using the power of the Void.",
            "Crafted with 8 Void Fragments and an Aspect of the Void.");
    public static final Item NECRONS_SCYTHE = register("necrons_scythe", "Necron's Scythe", Items.DIAMOND_HOE,
            "Ability: Wither Impact  RIGHT CLICK",
            "Slash forward with a sweeping strike, damaging nearby enemies.",
            "Mana Cost: 50",
            "Cooldown: 4 ticks");
    public static final Item ATOMSPLIT_KATANA = register("atomsplit_katana", "Atomsplit Katana", Items.NETHERITE_SWORD,
            "Damage: 305", "Strength: 100", "Crit Damage: 50%", "Intelligence: 300");
    public static final Item LIVID_DAGGER = register("livid_dagger", "Livid Dagger", Items.DIAMOND_SWORD,
            "Damage: 210", "Strength: 60", "Crit Chance: 100%", "Crit Damage: 50%");
    public static final Item DARK_CLAYMORE = register("dark_claymore", "Dark Claymore", Items.NETHERITE_SWORD,
            "Damage: 500", "Strength: 100", "Crit Damage: 100%");
    public static final Item HALBERD_OF_THE_SHREDDED = register("halberd_of_the_shredded", "Halberd of the Shredded", Items.NETHERITE_AXE,
            "Damage: 140", "Strength: 115");
    public static final Item BLADE_HANDLE = register("blade_handle", "Blade Handle", Items.STICK);
    public static final Item IRON_ALLOY = registerStackableGlint("iron_alloy", "Iron Alloy", Items.IRON_BLOCK);
    public static final Item DRILL_BIT = registerStackableGlint("drill_bit", "Drill Bit", Items.PRISMARINE_SHARD);
    public static final Item RUBY_DRILL = register("ruby_drill", "Ruby Drill", Items.PRISMARINE_SHARD);
    public static final Item AMETHYST_DRILL = register("amethyst_drill", "Amethyst Drill", Items.PRISMARINE_SHARD);
    public static final Item AMBER_DRILL = register("amber_drill", "Amber Drill", Items.PRISMARINE_SHARD);
    public static final Item TOPAZ_DRILL = register("topaz_drill", "Topaz Drill", Items.PRISMARINE_SHARD);
    public static final Item SAPPHIRE_DRILL = register("sapphire_drill", "Sapphire Drill", Items.PRISMARINE_SHARD);
    public static final Item JADE_DRILL = register("jade_drill", "Jade Drill", Items.PRISMARINE_SHARD);
    public static final Item OPAL_DRILL = register("opal_drill", "Opal Drill", Items.PRISMARINE_SHARD);
    public static final Item JASPER_DRILL = register("jasper_drill", "Jasper Drill", Items.PRISMARINE_SHARD);
    public static final Item GEMSTONE_GAUNTLET = register("gemstone_gauntlet", "Gemstone Gauntlet", Items.PRISMARINE_SHARD);
    public static final Item ENCHANTED_DIAMOND = registerStackableGlint("enchanted_diamond", "Enchanted Diamond", Items.DIAMOND, "Needs 160 diamonds to craft");
    public static final Item ENCHANTED_DIAMOND_BLOCK = registerStackableGlint("enchanted_diamond_block", "Enchanted Diamond Block", Items.DIAMOND_BLOCK, "Needs 80 enchanted diamonds to craft");
    public static final Item ZOMBIE_SWORD = register("zombie_sword", "Zombie Sword", Items.GOLDEN_SWORD,
            "Damage: 100", "Strength: 50", "Intelligence: 50",
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
            "Click in your inventory to view your stats, including your held weapon.");
    public static final Item ADMIN_RING = registerProfileHead("admin_ring", "Admin Ring", null,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQwZTdjMDE2YTgzYzdjNTgwNmI4ZWZmMGZhMTJmMjczYzAzNmExOGU1MDY2YmRlZDI4MGZhNmJlZGU3Y2M5ZCJ9fX0=", "Accessory", "+100% Damage", "All Stats: x5");

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

    private static Item registerStackableGlint(String path, String name, Item fallback, String... loreLines) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(64)
                .component(DataComponents.CUSTOM_NAME, ItemFormatting.name(name, ItemFormatting.rarityColor(path)))
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (loreLines.length > 0) {
            List<Component> lore = Arrays.stream(loreLines)
                    .<Component>map(ItemFormatting::lore)
                    .toList();
            properties.component(DataComponents.LORE, new ItemLore(lore));
        }
        Item item = isTexturedWeapon(path)
                ? new TexturedPolymerItem(properties, fallback,
                        Identifier.fromNamespaceAndPath(NAMESPACE, path))
                : new eu.pb4.polymer.core.api.item.SimplePolymerItem(properties, fallback);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

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
                .component(DataComponents.ATTRIBUTE_MODIFIERS, fallbackAttributeModifiers(fallback));
        if (isGemstoneDrill(path)) {
            properties.enchantable(15);
        }
        // Explicitly point the client at this mod's item definition so Polymer
        // sends the custom weapon model instead of only the vanilla fallback.
        if (isTexturedWeapon(path)) {
            properties.component(DataComponents.ITEM_MODEL,
                    Identifier.fromNamespaceAndPath(NAMESPACE, path));
        }
        if (loreLines.length > 0) {
            List<Component> lore = Arrays.stream(loreLines)
                    .<Component>map(ItemFormatting::lore)
                    .toList();
            properties.component(DataComponents.LORE, new ItemLore(lore));
        }
        Item item = isTexturedWeapon(path)
                ? new TexturedPolymerItem(properties, fallback,
                        Identifier.fromNamespaceAndPath(NAMESPACE, path))
                : new eu.pb4.polymer.core.api.item.SimplePolymerItem(properties, fallback);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }


    private static boolean isGemstoneDrill(String path) {
        return switch (path) {
            case "ruby_drill", "amethyst_drill", "amber_drill", "topaz_drill",
                    "sapphire_drill", "jade_drill", "opal_drill", "jasper_drill" -> true;
            default -> false;
        };
    }

    private static boolean isTexturedWeapon(String path) {
        return switch (path) {
            case "drill_bit", "ruby_drill", "amethyst_drill", "amber_drill", "topaz_drill",
                    "sapphire_drill", "jade_drill", "opal_drill", "jasper_drill", "gemstone_gauntlet",
                    "aotv", "aotd", "astraea", "hyperion", "voidsplit_katana",
                    "livid_dagger", "dark_claymore", "halberd_of_the_shredded" -> true;
            default -> false;
        };
    }

    private static ItemAttributeModifiers fallbackAttributeModifiers(Item fallback) {
        // Never inspect fallback.components() during item registration: the component
        // registry is not bound yet at this point in startup.
        if (fallback == Items.DIAMOND_SWORD) return swordAttributes(6.0, -2.4);
        if (fallback == Items.NETHERITE_SWORD) return swordAttributes(7.0, -2.4);
        if (fallback == Items.GOLDEN_SWORD) return swordAttributes(4.0, -2.4);
        if (fallback == Items.NETHERITE_AXE) return swordAttributes(10.0, -3.1);
        if (fallback == Items.DIAMOND_HOE) return swordAttributes(1.0, -3.0);
        return ItemAttributeModifiers.EMPTY;
    }

    private static ItemAttributeModifiers swordAttributes(double attackDamage, double attackSpeed) {
        return ItemAttributeModifiers.EMPTY
                .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("minecraft", "base_attack_damage"),
                                attackDamage,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .withModifierAdded(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("minecraft", "base_attack_speed"),
                                attackSpeed,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("hypixelcustomrecipe", "melee_hit_placeholder"),
                                0.01,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                );
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
