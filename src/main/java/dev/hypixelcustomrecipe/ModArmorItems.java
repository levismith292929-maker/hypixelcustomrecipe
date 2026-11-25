package dev.hypixelcustomrecipe;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

public final class ModArmorItems {
    private static final String NAMESPACE = "hypixel";
    private static final ArmorMaterial MATERIAL = createMaterial();
    private static final Map<String, Item> REGISTERED = new java.util.LinkedHashMap<>();
    private static final Map<String, Stats> REGISTERED_STATS = new java.util.LinkedHashMap<>();
    private static final Map<String, Integer> VANILLA_ARMOR_STRENGTH = Map.of(
            "chainmail_helmet", 4, "chainmail_chestplate", 8,
            "chainmail_leggings", 6, "chainmail_boots", 3
    );
    private static final Map<String, Integer> VANILLA_ARMOR_DEFENSE = Map.ofEntries(
            Map.entry("leather_helmet", 5), Map.entry("leather_chestplate", 10),
            Map.entry("leather_leggings", 7), Map.entry("leather_boots", 3),
            Map.entry("golden_helmet", 10), Map.entry("golden_chestplate", 18),
            Map.entry("golden_leggings", 15), Map.entry("golden_boots", 7),
            Map.entry("chainmail_helmet", 11), Map.entry("chainmail_chestplate", 19),
            Map.entry("chainmail_leggings", 16), Map.entry("chainmail_boots", 8),
            Map.entry("iron_helmet", 13), Map.entry("iron_chestplate", 21),
            Map.entry("iron_leggings", 18), Map.entry("iron_boots", 10),
            Map.entry("diamond_helmet", 17), Map.entry("diamond_chestplate", 30),
            Map.entry("diamond_leggings", 24), Map.entry("diamond_boots", 13),
            Map.entry("netherite_helmet", 23), Map.entry("netherite_chestplate", 36),
            Map.entry("netherite_leggings", 30), Map.entry("netherite_boots", 19),
            Map.entry("copper_helmet", 9), Map.entry("copper_chestplate", 17),
            Map.entry("copper_leggings", 14), Map.entry("copper_boots", 6)
    );

    private ModArmorItems() {
    }


    public static int getVanillaArmorDefense(String itemPath) {
        return VANILLA_ARMOR_DEFENSE.getOrDefault(itemPath, 0);
    }

    public static int getVanillaArmorStrength(String itemPath) {
        return VANILLA_ARMOR_STRENGTH.getOrDefault(itemPath, 0);
    }

    public static void initialize() {
        if (!REGISTERED.isEmpty()) {
            return;
        }

        // Dragon armor — Holy Dragon is intentionally excluded, matching the existing armor lineup.
        registerSet(new ArmorSet(
                "old_dragon", "Old Dragon", 0xAA00AA, 11184810,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTI0YzRkYTZhYjI3ZTljOTNmYmY0OWVhMWIxZmVmYWVmYmUyZDRjMGM4YjIyMmUyZmY2YjE4MTc5ZmI1YWRkZiJ9fX0=",
                new Stats(507,110,0,0), new Stats(832,160,0,0), new Stats(728,140,0,0), new Stats(442,90,0,0)
        ));
        registerSet(new ArmorSet(
                "protector_dragon", "Protector Dragon", 0xAA00AA, 10066329,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWYxYTFjY2JiMDI0ZWQxZDI4NDcxYmM0YjNjMjhkMDkzMjc3MTYxYTM4NzFkMjY5OWQ2OGU1ZDJjMzg3OTY0In19fQ==",
                new Stats(390,135,0,0), new Stats(663,185,0,0), new Stats(559,165,0,0), new Stats(338,115,0,0)
        ));
        registerSet(new ArmorSet(
                "strong_dragon", "Strong Dragon", 0xAA00AA, 11141290,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzhiYzY1NTFiZWIyMTQ4ZDkzOWI2OTkyNTE0MGQyMzc5ZmUwZmFiNmU3ZDMxZjY1ZGY1MGI1OWU5Zjc3YTBlNCJ9fX0=",
                new Stats(390,110,25,0), new Stats(663,160,25,0), new Stats(559,140,25,0), new Stats(338,90,25,0)
        ));
        registerSet(new ArmorSet(
                "superior_dragon", "Superior Dragon", 0xFFAA00, 16766720,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWUwMzI5ZjQyMTkyZWU3YzFhMGM3MDRmODJkYmJiZTdjMDBmYmZhM2YwMjBjMTA2N2EyMDg2MzBiOTkxYjk4OCJ9fX0=",
                new Stats(507,130,10,25,2,3), new Stats(832,190,10,25,2,3), new Stats(728,170,10,25,2,3), new Stats(442,110,10,25,2,3)
        ));
        registerSet(new ArmorSet(
                "unstable_dragon", "Unstable Dragon", 0xAA00AA, 5570634,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjA1ZmM0ZTJiNjhjNDUwNTlhZmVmMzk5OWU5YTk1YWEzZDg5NDM1OTdhNzg5NGU1NjkzOTdkNmNmNWQxNjcyYSJ9fX0=",
                new Stats(390,110,0,25,1,4), new Stats(663,160,0,0,1,4), new Stats(559,140,0,0,1,4), new Stats(338,90,0,0,1,4)
        ));
        registerSet(new ArmorSet(
                "wise_dragon", "Wise Dragon", 0xAA00AA, 5636095,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTc5N2M3N2NjZWUzYzA0YzNkZWVkNzcwNTk1ZWE4M2VhMWU4NGM5YTQ2MjliNzUzYWY1ZDM1MWMwY2MxMWE3NCJ9fX0=",
                new Stats(390,110,0,125), new Stats(663,160,0,75), new Stats(559,140,0,75), new Stats(338,90,0,75)
        ));
        registerSet(new ArmorSet(
                "young_dragon", "Young Dragon", 0xAA00AA, 5635925,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTBlODFlZDA3ZGZiMDI0NGQ1NmY0ZDVmMmIzNzU1M2VjMDI2ZmI0Nzk2ZjBmYjBjNTdhOGViMjY0OTgzZTFlMCJ9fX0=",
                new Stats(390,110,0,0), new Stats(663,160,0,0), new Stats(559,140,0,0), new Stats(338,90,0,0)
        ));

        // Necron armor keeps its custom player-head helmet and its per-piece leather dye colours.
        registerSet(new ArmorSet(
                "necron", "Necron's", 0xFFD000, 15155516,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmJiYjJmYTdhNmNhMDg3MjgwZWEwY2I1NjRiNDFlZjFhZTQwNGExOWY3YTgxMjhkM2QyOGM1MTlhODVlMDYzZiJ9fX0=",
                new Stats(500,150,15,80), new Stats(650,200,15,90), new Stats(550,175,15,85), new Stats(350,125,15,75),
                new int[]{15155516,15162428,15162428,15167036}, true
        ));
    }

    public static int getCriticalChance(net.minecraft.world.entity.player.Player player) {
        int chance = 5;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            Stats stats = getStats(stack);
            if (stats != null) chance += stats.critChance();
        }
        return Math.min(chance, 100);
    }

    public static int getCriticalDamage(net.minecraft.world.entity.player.Player player) {
        int damage = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            Stats stats = getStats(stack);
            if (stats != null) damage += stats.critDamage();
        }
        return damage;
    }

    /**
     * Collect the registered custom armor stats worn by a player.
     * This is the single equipment aggregation entry point used by the
     * migrated stat engine, HUD, and Stat Viewer.
     */
    public static SkyblockStats getSkyblockStats(net.minecraft.world.entity.player.Player player) {
        int health = SkyblockStats.BASE.health();
        int defense = SkyblockStats.BASE.defense();
        int strength = SkyblockStats.BASE.strength();
        int armorStrength = SkyblockStats.BASE.armorStrength();
        int intelligence = SkyblockStats.BASE.intelligence();
        int critChance = SkyblockStats.BASE.critChance();
        int critDamage = SkyblockStats.BASE.critDamage();

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack equipped = player.getItemBySlot(slot);
            Stats piece = getStats(equipped);
            if (piece != null) {
                health += piece.health();
                defense += piece.defense();
                strength += piece.strength();
                intelligence += piece.intelligence();
                critChance += piece.critChance();
                critDamage += piece.critDamage();
            } else if (!equipped.isEmpty()) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(equipped.getItem());
                defense += VANILLA_ARMOR_DEFENSE.getOrDefault(itemId.getPath(), 0);
                armorStrength += VANILLA_ARMOR_STRENGTH.getOrDefault(itemId.getPath(), 0);
            }
        }

        return new SkyblockStats(
                health, defense, strength, armorStrength, 0, critChance, critDamage,
                intelligence, SkyblockStats.BASE.manaMaximum() + intelligence,
                SkyblockStats.BASE.manaRegeneration() + (intelligence + 2) / 4, 1.0
        );
    }

    /** True only when all four Necron armor slots are equipped. */
    public static boolean isWearingFullNecronSet(net.minecraft.world.entity.player.Player player) {
        return isSetPiece(player, EquipmentSlot.HEAD, "necron_helmet")
                && isSetPiece(player, EquipmentSlot.CHEST, "necron_chestplate")
                && isSetPiece(player, EquipmentSlot.LEGS, "necron_leggings")
                && isSetPiece(player, EquipmentSlot.FEET, "necron_boots");
    }

    private static boolean isSetPiece(net.minecraft.world.entity.player.Player player,
                                      EquipmentSlot slot, String baseId) {
        String path = BuiltInRegistries.ITEM.getKey(player.getItemBySlot(slot).getItem()).getPath();
        return path.equals(baseId) || (path.startsWith(baseId + "_")
                && path.substring((baseId + "_").length()).matches("[0-9]+star"));
    }

    private static Stats getStats(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!NAMESPACE.equals(id.getNamespace())) return null;
        return REGISTERED_STATS.get(id.getPath());
    }

    public static Item get(String id) {
        return REGISTERED.get(id);
    }

    public static Map<String, Item> all() {
        return Map.copyOf(REGISTERED);
    }

    private static void registerSet(ArmorSet set) {
        registerPiece(set, "helmet", "Helmet", ArmorType.HELMET, Items.PLAYER_HEAD, set.helmetTexture, set.pieceStats[0], set.dyeFor(0));
        registerPiece(set, "chestplate", "Chestplate", ArmorType.CHESTPLATE, Items.LEATHER_CHESTPLATE, null, set.pieceStats[1], set.dyeFor(1));
        registerPiece(set, "leggings", "Leggings", ArmorType.LEGGINGS, Items.LEATHER_LEGGINGS, null, set.pieceStats[2], set.dyeFor(2));
        registerPiece(set, "boots", "Boots", ArmorType.BOOTS, Items.LEATHER_BOOTS, null, set.pieceStats[3], set.dyeFor(3));
    }

    private static void registerPiece(ArmorSet set, String slotId, String slotName, ArmorType type,
                                      Item fallback, String profileTexture, Stats baseStats, int dye) {
        String baseId = set.id + "_" + slotId;
        for (int stars = 0; stars <= 3; stars++) {
            String itemId = stars == 0 ? baseId : baseId + "_" + stars + "star";
            String displayName = set.displayName + " " + slotName + (stars == 0 ? "" : " " + "★".repeat(stars));
            Stats scaled = baseStats.scale(stars);
            List<Component> lore = new ArrayList<>();
            lore.add(ItemFormatting.lore("Health: " + Integer.toString((int) Math.round(scaled.health / 10.0))));
            lore.add(ItemFormatting.lore("Defense: " + scaled.defense));
            if (scaled.strength != 0) {
                lore.add(ItemFormatting.lore("Strength: " + scaled.strength));
            }
            if (scaled.intelligence != 0) {
                lore.add(ItemFormatting.lore("Intelligence: " + scaled.intelligence));
            }
            if (scaled.critChance != 0) {
                lore.add(ItemFormatting.lore("Crit Chance: " + scaled.critChance + "%"));
            }
            if (scaled.critDamage != 0) {
                lore.add(ItemFormatting.lore("Crit Damage: " + scaled.critDamage + "%"));
            }
            if (set.necronBonus) {
                lore.add(ItemFormatting.lore("Full Set Bonus: Necron's Guards"));
                lore.add(ItemFormatting.lore("When hit, summons two armored wither skeletons."));
                lore.add(ItemFormatting.lore("Cooldown: 30 seconds"));
            }

            ResourceKey<Item> key = ResourceKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(NAMESPACE, itemId)
            );
            Item.Properties properties = new Item.Properties()
                    .setId(key)
                    .stacksTo(1);
            if (type == ArmorType.HELMET) {
                // Keep the helmet's player-head rendering/profile instead of applying a leather helmet layer.
                properties.component(DataComponents.EQUIPPABLE,
                        Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build());
            } else {
                properties.humanoidArmor(MATERIAL, type);
            }
            properties.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                    .component(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, new LinkedHashSet<>(Set.of(DataComponents.UNBREAKABLE))))
                    .component(DataComponents.CUSTOM_NAME, ItemFormatting.name(displayName, set.nameColor))
                    .component(DataComponents.LORE, new ItemLore(lore));
            if (type != ArmorType.HELMET) {
                properties.component(DataComponents.DYED_COLOR, new DyedItemColor(dye));
            }
            if (profileTexture != null) {
                properties.component(DataComponents.PROFILE, createProfile(profileTexture, itemId));
            }

            Item item = new SimplePolymerItem(properties, fallback) {
                @Override
                public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
                    var profile = stack.get(DataComponents.PROFILE);
                    if (profile != null) {
                        out.set(DataComponents.PROFILE, profile);
                    }
                    var dyedColor = stack.get(DataComponents.DYED_COLOR);
                    if (dyedColor != null) {
                        out.set(DataComponents.DYED_COLOR, dyedColor);
                    }
                }
            };
            Registry.register(BuiltInRegistries.ITEM, key, item);
            REGISTERED.put(itemId, item);
            REGISTERED_STATS.put(itemId, scaled);
        }
    }

    private static ResolvableProfile createProfile(String textureValue, String itemId) {
        Property texture = new Property("textures", textureValue);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("hypixel:" + itemId).getBytes(StandardCharsets.UTF_8)),
                "Hypixel",
                properties
        );
        return ResolvableProfile.createResolved(profile);
    }

    private static ArmorMaterial createMaterial() {
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.HELMET, 0);
        defense.put(ArmorType.CHESTPLATE, 0);
        defense.put(ArmorType.LEGGINGS, 0);
        defense.put(ArmorType.BOOTS, 0);
        TagKey<Item> repairTag = TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath("minecraft", "repairs_leather_armor")
        );
        return new ArmorMaterial(
                5,
                defense,
                15,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                0.0f,
                0.0f,
                repairTag,
                EquipmentAssets.LEATHER
        );
    }

    private record ArmorSet(
            String id,
            String displayName,
            int nameColor,
            String helmetTexture,
            Stats[] pieceStats,
            int[] perPieceDyes,
            boolean necronBonus
    ) {
        ArmorSet(String id, String displayName, int nameColor, int dyeColor, String helmetTexture,
                 Stats helmet, Stats chestplate, Stats leggings, Stats boots) {
            this(id, displayName, nameColor, helmetTexture,
                    new Stats[]{helmet, chestplate, leggings, boots},
                    new int[]{dyeColor, dyeColor, dyeColor, dyeColor}, false);
        }

        ArmorSet(String id, String displayName, int nameColor, int dyeColor, String helmetTexture,
                 Stats helmet, Stats chestplate, Stats leggings, Stats boots,
                 int[] perPieceDyes, boolean necronBonus) {
            this(id, displayName, nameColor, helmetTexture,
                    new Stats[]{helmet, chestplate, leggings, boots}, perPieceDyes, necronBonus);
        }

        int dyeFor(int index) {
            return perPieceDyes[index];
        }
    }

    private record Stats(int health, int defense, int strength, int intelligence, int critChance, int critDamage) {
        Stats(int health, int defense, int strength, int intelligence) {
            this(health, defense, strength, intelligence, 0, 0);
        }

        Stats scale(int stars) {
            double multiplier = switch (stars) {
                case 1 -> 1.10;
                case 2 -> 1.25;
                case 3 -> 1.45;
                default -> 1.0;
            };
            return new Stats(
                    (int) Math.round(health * multiplier),
                    (int) Math.round(defense * multiplier),
                    (int) Math.round(strength * multiplier),
                    (int) Math.round(intelligence * multiplier),
                    (int) Math.round(critChance * multiplier),
                    (int) Math.round(critDamage * multiplier)
            );
        }
    }
}
