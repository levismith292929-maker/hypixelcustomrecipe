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
            "Ability: Instant Transmission", "Teleport to the block you are aiming at (up to 64 blocks).", "Mana Cost: 50");
    public static final Item AOTD = register("aotd", "Aspect of the Dragon", Items.NETHERITE_SWORD,
            "Ability: Dragon Rage", "Breathe fire 5 blocks forward; each mob hit takes 10 damage.", "Mana Cost: 80");
    public static final Item ASTRAEA = register("astraea", "Astraea", Items.NETHERITE_SWORD,
            "Ability: Wither Shield", "Reduce incoming damage and gain an absorption shield for 10 seconds.", "Mana Cost: 40");
    public static final Item HYPERION = register("hyperion", "Hyperion", Items.NETHERITE_SWORD,
            "Ability: Implosion", "Deal 60 damage to every mob within 5 blocks.", "Mana Cost: 50");
    public static final Item NECRONS_SCYTHE = register("necrons_scythe", "Necron's Scythe", Items.DIAMOND_HOE,
            "Ability: Wither Impact", "Slash forward, damaging nearby enemies in front of you.", "Mana Cost: 35");
    public static final Item ZOMBIE_SWORD = register("zombie_sword", "Zombie Sword", Items.GOLDEN_SWORD,
            "Ability: Instant Healing", "Instantly heals 1 heart.", "Mana Cost: 50", "Cooldown: 1 second");
    public static final Item ORE_TRACKER = register("ore_tracker", "Ore Tracker", Items.COMPASS,
            "Ability: Ore Search", "Find the nearest diamond ore or ancient debris within 32 blocks.", "Mana Cost: 100", "Cooldown: 30 seconds");
    public static final Item STAT_VIEWER = register("stat_viewer", "View Stats", Items.NETHER_STAR,
            "Right-click to view your current SkyBlock stats.");

    public static final Item PET_ENDERMAN = registerProfileHead("pet_enderman", "Enderman Pet", "MHF_Enderman", null,
            "Ability: Summon Pet", "Max Mana: 1000", "Mana Regeneration: 50/sec", "Damage: -20%");
    public static final Item PET_ENDER_DRAGON = registerProfileHead("pet_ender_dragon", "Ender Dragon Pet", null,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWVjM2ZmNTYzMjkwYjEzZmYzYmNjMzY4OThhZjdlYWE5ODhiNmNjMThkYzI1NDE0N2Y1ODM3NGFmZTliMjFiOSJ9fX0=",
            "Ability: Summon Pet", "Max Mana: 300", "Mana Regeneration: 20/sec", "Damage: +15%");
    public static final Item PET_GOLDEN_DRAGON = registerProfileHead("pet_golden_dragon", "Golden Dragon Pet", null,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmU5ZjliMWZjMDE0MTY2Y2I0NmEwOTNlNTM0OWIyYmY2ZWRkMjAxYjY4MGQ2MmU0OGRiZjNhZjliMDQ1OTExNiJ9fX0=",
            "Ability: Summon Pet", "Damage: +40%", "Max Mana: 750", "Mana Regeneration: 10/sec");
    public static final Item PET_IRON_GOLEM = registerProfileHead("pet_iron_golem", "Iron Golem Pet", "MHF_Golem", null,
            "Ability: Summon Pet", "Damage: +5%", "Max Mana: 600", "Mana Regeneration: 30/sec", "Permanent Resistance I");
    public static final Item PET_REMOVER = registerProfileHead("pet_remover", "Pet Remover", "MHF_TNT2", null,
            "Ability: Dismiss Pet", "Removes your active pet and restores your normal mana stats.");

    private DatapackItems() {}

    private static Item register(String path, String name, Item fallback, String... loreLines) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (loreLines.length > 0) {
            List<Component> lore = Arrays.stream(loreLines)
                    .<Component>map(Component::literal)
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
                .component(DataComponents.CUSTOM_NAME, Component.literal(name))
                .component(DataComponents.PROFILE, profile);
        if (loreLines.length > 0) {
            List<Component> lore = Arrays.stream(loreLines).<Component>map(Component::literal).toList();
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
