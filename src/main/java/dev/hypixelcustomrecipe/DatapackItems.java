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
    public static final Item SOUL_BLADE = register("soul_blade", "Soul Blade", Items.NETHERITE_HOE,
            "Ability: Soul Slash  RIGHT CLICK",
            "Slash forward, dealing 5% of each target's Max Health as true damage.",
            "Mana Cost: 100",
            "Cooldown: 1 tick");
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
    public static final Item COMPACT_IRON_INGOT = registerStackableGlint("compact_iron_ingot", "Compact Iron Ingot", Items.IRON_INGOT);
    public static final Item COMPACT_IRON_BLOCK = registerStackableGlint("compact_iron_block", "Compact Iron Block", Items.IRON_BLOCK);
    public static final Item COMPACT_GOLD_INGOT = registerStackableGlint("compact_gold_ingot", "Compact Gold Ingot", Items.GOLD_INGOT);
    public static final Item COMPACT_GOLD_BLOCK = registerStackableGlint("compact_gold_block", "Compact Gold Block", Items.GOLD_BLOCK);
    public static final Item HUNTER_KNIFE = register("hunter_knife", "Hunter Knife", Items.IRON_SWORD,
            "A compact, deadly hunting blade.");
    public static final Item RAIDER_AXE = register("raider_axe", "Raider Axe", Items.IRON_AXE,
            "A sturdy axe favored by raiders.");
    public static final Item VENGENCE = register("vengence", "Vengence", Items.NETHERITE_AXE,
            "Damage: 50", "Strength: 25", "Crit Chance: -100%",
            "Ability: Bloodlust",
            "Each kill multiplies Damage and Strength by 1.05x for 3 seconds.",
            "Kills refresh the duration. No stack limit.");
    public static final Item BLADE_HANDLE = register("blade_handle", "Blade Handle", Items.STICK);
    public static final Item IRON_ALLOY = registerStackableGlint("iron_alloy", "Iron Alloy", Items.IRON_BLOCK);
    public static final Item DRILL_BIT = registerStackableGlint("drill_bit", "Drill Bit", Items.PRISMARINE_SHARD);
    public static final Item RUBY_DRILL = registerMiningTool("ruby_drill", "Ruby Drill", "Ruby–Amethyst");
    public static final Item AMETHYST_DRILL = registerMiningTool("amethyst_drill", "Amethyst Drill", "Ruby–Amber");
    public static final Item AMBER_DRILL = registerMiningTool("amber_drill", "Amber Drill", "Ruby–Topaz");
    public static final Item TOPAZ_DRILL = registerMiningTool("topaz_drill", "Topaz Drill", "Ruby–Sapphire");
    public static final Item SAPPHIRE_DRILL = registerMiningTool("sapphire_drill", "Sapphire Drill", "Ruby–Jade");
    public static final Item JADE_DRILL = registerMiningTool("jade_drill", "Jade Drill", "Ruby–Opal");
    public static final Item OPAL_DRILL = registerMiningTool("opal_drill", "Opal Drill", "Ruby–Jasper");
    public static final Item JASPER_DRILL = registerMiningTool("jasper_drill", "Jasper Drill", "All Gemstones");
    public static final Item ADMIN_DRILL = register("admin_drill", "Admin Drill", Items.PRISMARINE_SHARD,
            "Admin Item", "Instantly mines any block.");
    public static final Item GEMSTONE_GAUNTLET = registerMiningTool("gemstone_gauntlet", "Gemstone Gauntlet", "All Gemstones");
    public static final Item ENCHANTED_DIAMOND = registerStackableGlint("enchanted_diamond", "Enchanted Diamond", Items.DIAMOND, "Needs 160 diamonds to craft");
    public static final Item ENCHANTED_DIAMOND_BLOCK = registerStackableGlint("enchanted_diamond_block", "Enchanted Diamond Block", Items.DIAMOND_BLOCK, "Needs 80 enchanted diamonds to craft");
    public static final Item ZOMBIE_SWORD = register("zombie_sword", "Zombie Sword", Items.GOLDEN_SWORD,
            "Damage: 100", "Strength: 50", "Intelligence: 50",
            "Ability: Instant Healing  RIGHT CLICK", "Heals 5% of your maximum Health.",
            "Mana Cost: 50", "Cooldown: 1 second");
    public static final Item ORNATE_ZOMBIE_SWORD = registerHealingSword("ornate_zombie_sword", "Ornate Zombie Sword",
            "Ability: Instant Healing  RIGHT CLICK", "Heals 15% of your maximum Health.",
            "Heals other players for 2% of their maximum Health.",
            "Base healing radius: 2 blocks (+0.1 blocks per Intelligence digit).",
            "Mana Cost: 100", "Cooldown: 20 ticks");
    public static final Item EMBELLISHED_ZOMBIE_SWORD = registerHealingSword("embellished_zombie_sword", "Embellished Zombie Sword",
            "Ability: Instant Healing  RIGHT CLICK", "Heals 25% of your maximum Health.",
            "Heals other players for 6% of their maximum Health.",
            "Base healing radius: 2.5 blocks (+0.2 blocks per Intelligence digit).",
            "Mana Cost: 300", "Cooldown: 10 seconds");
    public static final Item ORE_TRACKER = register("ore_tracker", "Ore Tracker", Items.COMPASS,
            "Ability: Ore Search  RIGHT CLICK",
            "Find the nearest diamond ore or ancient debris",
            "within 32 blocks in your current dimension.",
            "Mana Cost: 100",
            "Cooldown: 30 seconds");
    public static final Item STAT_VIEWER = register("stat_viewer", "View Stats", Items.NETHER_STAR,
            "Click in your inventory to view your stats, including your held weapon.");
    public static final Item ZOMBIE_RING = register("zombie_ring", "Zombie Ring", Items.ROTTEN_FLESH,
            "Accessory", "Take 10% less damage from Undead mobs.");
    public static final Item DEVOUR_RING = register("devour_ring", "Devour Ring", Items.GOLDEN_APPLE,
            "Accessory", "Heal 5 Health when you kill a mob.");
    public static final Item ZOMBIE_ARTIFACT = register("zombie_artifact", "Zombie Artifact", Items.ZOMBIE_HEAD,
            "Accessory", "Take 15% less damage from Undead mobs.");
    public static final Item REAPER_ORB = register("reaper_orb", "Reaper Orb", Items.WITHER_SKELETON_SKULL,
            "Accessory", "Gain +2 Strength for each mob killed in the last 5 seconds.");
    public static final Item SPIDER_RING = register("spider_ring", "Spider Ring", Items.SPIDER_EYE,
            "Accessory", "Take 10% less damage from Arthropods.");
    public static final Item TARANTULA_TALISMAN = register("tarantula_talisman", "Tarantula Talisman", Items.FERMENTED_SPIDER_EYE,
            "Accessory", "Every 10th melee hit against the same enemy deals 10% extra damage.");
    public static final Item SPIDER_ARTIFACT = register("spider_artifact", "Spider Artifact", Items.COBWEB,
            "Accessory", "Take 15% less damage from Arthropods.");
    public static final Item RED_CLAW_TALISMAN = register("red_claw_talisman", "Red Claw Talisman", Items.REDSTONE,
            "Accessory", "+1% Crit Damage.");
    public static final Item RED_CLAW_RING = register("red_claw_ring", "Red Claw Ring", Items.REDSTONE,
            "Accessory", "+3% Crit Damage.");
    public static final Item RED_CLAW_ARTIFACT = register("red_claw_artifact", "Red Claw Artifact", Items.REDSTONE_BLOCK,
            "Accessory", "+5% Crit Damage.");
    public static final Item HUNTER_TALISMAN = register("hunter_talisman", "Hunter Talisman", Items.BONE,
            "Accessory", "+2 Combat XP per mob kill.");
    public static final Item HUNTER_RING = register("hunter_ring", "Hunter Ring", Items.BONE_BLOCK,
            "Accessory", "+5 Combat XP per mob kill.");
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
    public static final Item MAXOR_PET = register("maxor_pet", "Maxor Pet", Items.WITHER_SKELETON_SKULL,
            "Pet Ability: Hardened Presence",
            "+100% Defense",
            "+10 True Defense",
            "Necron's Guard summons 20 guards instead of 2 when ready.");
    public static final Item STORM_PET = register("storm_pet", "Storm Pet", Items.SKELETON_SKULL,
            "Pet Ability: Soul Surge",
            "+5 True Defense",
            "Soul Blade: +0.5 percentage points against Wither Trial enemies",
            "Soul Blade: +6.5 percentage points against other enemies",
            "Soul Blade is returned to you when you die.");
    public static final Item GOLDOR_PET = register("goldor_pet", "Goldor Pet", Items.ZOMBIE_HEAD,
            "Pet Ability: Swordfall",
            "+100% Strength",
            "-10% Intelligence",
            "+5 True Defense",
            "Every 3rd Dark Claymore hit summons a sword above the target",
            "that deals 15% of the target's maximum Health.");
    public static final Item NECRON_PET = register("necron_pet", "Necron Pet", Items.WITHER_SKELETON_SKULL,
            "Pet Ability: Withering Aura",
            "+25 True Defense",
            "+50% Damage",
            "+100% to every stat",
            "Soul Blade: +6.5 percentage points against Wither Trial enemies",
            "Soul Blade: +21.5 percentage points against other enemies",
            "Drains 1% of nearby enemies' maximum Health per second within 2 blocks",
            "Necron's Scythe has no mana cost or cooldown.");
    public static final Item PET_REMOVER = registerProfileHead("pet_remover", "Pet Remover", "MHF_TNT2", null,
            "Ability: Dismiss Pet", "Removes your active pet and restores your normal mana stats.");

    private DatapackItems() {}

    private static Item registerHealingSword(String path, String name, String... loreLines) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Identifier modelId = Identifier.fromNamespaceAndPath(NAMESPACE, path);
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.CUSTOM_NAME, ItemFormatting.name(name, ItemFormatting.rarityColor(path)))
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .component(DataComponents.ITEM_MODEL, modelId);
        if (loreLines.length > 0) {
            properties.component(DataComponents.LORE, new ItemLore(Arrays.stream(loreLines)
                    .<Component>map(ItemFormatting::lore).toList()));
        }
        return Registry.register(BuiltInRegistries.ITEM, key,
                new TexturedPolymerItem(properties, Items.GOLDEN_SWORD, modelId));
    }

    private static Item registerStackableGlint(String path, String name, Item fallback, String... loreLines) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(64)
                .component(DataComponents.CUSTOM_NAME, ItemFormatting.name(name, ItemFormatting.rarityColor(path)))
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, !isMiningTool(path))
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

    private static Item registerMiningTool(String path, String name, String gemstoneRange) {
        return register(path, name, Items.PRISMARINE_SHARD,
                "Mining Tool",
                "",
                "Gemstone Mining",
                "Can mine: " + gemstoneRange,
                "",
                "A specialized tool forged for gemstone mining.");
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
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, !isMiningTool(path))
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


    private static boolean isMiningTool(String path) {
        return switch (path) {
            case "drill_bit", "ruby_drill", "amethyst_drill", "amber_drill", "topaz_drill",
                    "sapphire_drill", "jade_drill", "opal_drill", "jasper_drill", "gemstone_gauntlet" -> true;
            default -> false;
        };
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
                    "livid_dagger", "dark_claymore", "halberd_of_the_shredded",
                    "zombie_sword", "ornate_zombie_sword", "embellished_zombie_sword" -> true;
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
