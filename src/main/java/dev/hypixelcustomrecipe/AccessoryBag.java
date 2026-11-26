package dev.hypixelcustomrecipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent 36-slot accessory storage and its four-row inventory screen. */
public final class AccessoryBag {
    private static final int SIZE = 36;
    private static final Map<UUID, NonNullList<ItemStack>> CONTENTS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Long>> RECENT_KILLS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Integer>> TARANTULA_HITS = new ConcurrentHashMap<>();
    private static final Codec<List<ItemStack>> STACK_LIST = ItemStack.CODEC.listOf();

    private AccessoryBag() {}

    public static boolean isAccessory(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(DatapackItems.ADMIN_RING)
                || stack.is(GemstoneAccessoryItems.RUBY_MASK)
                || stack.is(GemstoneAccessoryItems.AMETHYST_MASK)
                || stack.is(GemstoneAccessoryItems.AMBER_MASK)
                || stack.is(GemstoneAccessoryItems.TOPAZ_MASK)
                || stack.is(GemstoneAccessoryItems.SAPPHIRE_MASK)
                || stack.is(GemstoneAccessoryItems.JADE_MASK)
                || stack.is(GemstoneAccessoryItems.OPAL_MASK)
                || stack.is(GemstoneAccessoryItems.JASPER_MASK)
                || stack.is(GemstoneAccessoryItems.NATURE_GEM)
                || stack.is(DatapackItems.ZOMBIE_RING)
                || stack.is(DatapackItems.DEVOUR_RING)
                || stack.is(DatapackItems.ZOMBIE_ARTIFACT)
                || stack.is(DatapackItems.REAPER_ORB)
                || stack.is(DatapackItems.SPIDER_RING)
                || stack.is(DatapackItems.TARANTULA_TALISMAN)
                || stack.is(DatapackItems.SPIDER_ARTIFACT)
                || stack.is(DatapackItems.RED_CLAW_TALISMAN)
                || stack.is(DatapackItems.RED_CLAW_RING)
                || stack.is(DatapackItems.RED_CLAW_ARTIFACT)
                || stack.is(DatapackItems.HUNTER_TALISMAN)
                || stack.is(DatapackItems.HUNTER_RING));
    }

    public static SkyblockStats applyAdminRing(ServerPlayer player, SkyblockStats stats) {
        if (!hasAdminRing(player)) return stats;
        return new SkyblockStats(timesFive(stats.health()), timesFive(stats.defense()),
                timesFive(stats.strength()), timesFive(stats.armorStrength()), timesFive(stats.weaponStrength()),
                timesFive(stats.critChance()), timesFive(stats.critDamage()), timesFive(stats.intelligence()),
                timesFive(stats.manaMaximum()), timesFive(stats.manaRegeneration()), stats.weaponDamage() * 5.0);
    }

    /** Applies the unique gemstone accessories once; duplicates are rejected by the bag. */
    public static SkyblockStats applyGemstoneAccessories(ServerPlayer player, SkyblockStats original) {
        double health = 1.0, defense = 1.0, strength = 1.0, critChance = 1.0;
        double critDamage = 1.0, intelligence = 1.0, mana = 1.0, manaRegen = 1.0, weaponDamage = 1.0;

        if (has(player, GemstoneAccessoryItems.RUBY_MASK)) health += 0.10;

        if (has(player, GemstoneAccessoryItems.AMBER_MASK)) strength += 0.10;
        if (has(player, GemstoneAccessoryItems.TOPAZ_MASK)) critChance += 0.05;
        if (has(player, GemstoneAccessoryItems.SAPPHIRE_MASK)) intelligence += 0.15;
        if (has(player, GemstoneAccessoryItems.JADE_MASK)) defense += 0.20;
        if (has(player, GemstoneAccessoryItems.OPAL_MASK)) critDamage += 0.25;
        if (has(player, GemstoneAccessoryItems.JASPER_MASK)) {
            health += 0.04; defense += 0.04; strength += 0.04;
            critChance += 0.04; critDamage += 0.04; intelligence += 0.04;
            mana += 0.04; manaRegen += 0.04; weaponDamage += 0.04;
        }
        if (has(player, GemstoneAccessoryItems.NATURE_GEM)) {
            health += 0.02; defense += 0.02;
        }

        // Gem Tank contents only grant their auras while the Gem Tank itself is in the Accessory Bag.
        if (has(player, GemstoneAccessoryItems.GEM_TANK)) {
            health += GemTankMenu.qualityPercentFor(player, 0) / 100.0;
            strength += GemTankMenu.qualityPercentFor(player, 2) / 100.0;
            critChance += GemTankMenu.qualityPercentFor(player, 3) / 100.0;
            intelligence += GemTankMenu.qualityPercentFor(player, 4) / 100.0;
            defense += GemTankMenu.qualityPercentFor(player, 5) / 100.0;
            critDamage += GemTankMenu.qualityPercentFor(player, 6) / 100.0;
            double all = GemTankMenu.qualityPercentFor(player, 7) / 100.0;
            health += all; defense += all; strength += all;
            critChance += all; critDamage += all; intelligence += all;
            mana += all; manaRegen += all; weaponDamage += all;
        }

        int reaperStrength = has(player, DatapackItems.REAPER_ORB) ? recentKillCount(player) * 2 : 0;
        int redClawCritDamage = (has(player, DatapackItems.RED_CLAW_ARTIFACT) ? 5 : 0)
                + (has(player, DatapackItems.RED_CLAW_RING) ? 3 : 0)
                + (has(player, DatapackItems.RED_CLAW_TALISMAN) ? 1 : 0);

        return new SkyblockStats(
                scale(original.health(), health), scale(original.defense(), defense),
                scale(original.strength(), strength) + reaperStrength, scale(original.armorStrength(), strength),
                scale(original.weaponStrength(), strength), scale(original.critChance(), critChance),
                scale(original.critDamage(), critDamage) + redClawCritDamage, scale(original.intelligence(), intelligence),
                scale(original.manaMaximum(), mana), scale(original.manaRegeneration(), manaRegen),
                original.weaponDamage() * weaponDamage
        );
    }

    /** Records Slayer accessory kill effects and grants Devour Ring healing. */
    public static void onMobKill(ServerPlayer player) {
        long now = player.level().getGameTime();
        List<Long> kills = RECENT_KILLS.computeIfAbsent(player.getUUID(),
                ignored -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()));
        synchronized (kills) {
            kills.add(now);
            kills.removeIf(tick -> now - tick > 100);
        }
        if (has(player, DatapackItems.DEVOUR_RING)) {
            SkyblockPlayerState.get(player).heal(50);
        }
    }

    private static int recentKillCount(ServerPlayer player) {
        List<Long> kills = RECENT_KILLS.get(player.getUUID());
        if (kills == null) return 0;
        long now = player.level().getGameTime();
        synchronized (kills) {
            kills.removeIf(tick -> now - tick > 100);
            return kills.size();
        }
    }

    /** Additional flat Combat XP awarded for each mob kill. */
    public static int combatXpBonus(ServerPlayer player) {
        return (has(player, DatapackItems.HUNTER_TALISMAN) ? 2 : 0)
                + (has(player, DatapackItems.HUNTER_RING) ? 5 : 0);
    }

    /** Applies the Tarantula Talisman's every-tenth-hit bonus per target. */
    public static float applyTarantulaTalisman(ServerPlayer player, net.minecraft.world.entity.LivingEntity target,
                                               float damage) {
        if (!has(player, DatapackItems.TARANTULA_TALISMAN)) return damage;
        Map<UUID, Integer> hits = TARANTULA_HITS.computeIfAbsent(player.getUUID(),
                ignored -> new ConcurrentHashMap<>());
        int count = hits.merge(target.getUUID(), 1, Integer::sum);
        if (count >= 10) {
            hits.put(target.getUUID(), 0);
            return damage * 1.10F;
        }
        return damage;
    }

    /** Reduces incoming damage from Undead or Arthropod mobs while the matching accessory is worn. */
    public static float incomingDamageMultiplier(ServerPlayer player, net.minecraft.world.entity.LivingEntity attacker) {
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(attacker.getType()).getPath();
        boolean undead = java.util.Set.of("zombie", "husk", "drowned", "skeleton", "stray",
                "wither_skeleton", "zombie_villager", "zombified_piglin", "zoglin", "phantom",
                "wither", "skeleton_horse", "zombie_horse").contains(type);
        boolean arthropod = java.util.Set.of("spider", "cave_spider", "bee", "silverfish",
                "endermite").contains(type);
        double reduction = 0.0;
        if (undead) {
            if (has(player, DatapackItems.ZOMBIE_ARTIFACT)) reduction = 0.15;
            else if (has(player, DatapackItems.ZOMBIE_RING)) reduction = 0.10;
        } else if (arthropod) {
            if (has(player, DatapackItems.SPIDER_ARTIFACT)) reduction = 0.15;
            else if (has(player, DatapackItems.SPIDER_RING)) reduction = 0.10;
        }
        return (float) (1.0 - reduction);
    }

    /** Total additive Gemstone Fortune percentage from accessories and the Gem Tank. */
    public static double gemstoneFortuneBonusPercent(ServerPlayer player) {
        double bonus = 0.0;
        if (has(player, GemstoneAccessoryItems.AMETHYST_MASK)) bonus += 10.0;
        if (has(player, GemstoneAccessoryItems.NATURE_GEM)) bonus += 2.0;
        if (has(player, GemstoneAccessoryItems.JASPER_MASK)) bonus += 4.0;
        // Gem Tank Fortune is active only while the Gem Tank is in the Accessory Bag.
        if (has(player, GemstoneAccessoryItems.GEM_TANK)) {
            bonus += GemTankMenu.qualityPercentFor(player, 1);
            bonus += GemTankMenu.qualityPercentFor(player, 7);
        }
        return bonus;
    }

    private static int scale(int value, double multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value * multiplier));
    }

    private static boolean has(ServerPlayer player, Item item) {
        return contents(player).stream().anyMatch(stack -> stack.is(item));
    }

    public static double damageBonusMultiplier(ServerPlayer player) {
        return hasAdminRing(player) ? 2.0 : 1.0;
    }

    private static int timesFive(int value) {
        return (int) Math.min(Integer.MAX_VALUE, (long) value * 5L);
    }

    public static boolean hasAdminRing(ServerPlayer player) {
        NonNullList<ItemStack> items = contents(player);
        return items.stream().anyMatch(stack -> stack.is(DatapackItems.ADMIN_RING));
    }

    private static NonNullList<ItemStack> contents(ServerPlayer player) {
        return CONTENTS.computeIfAbsent(player.getUUID(), ignored ->
                NonNullList.withSize(SIZE, ItemStack.EMPTY));
    }

    public static void open(ServerPlayer player) {
        NonNullList<ItemStack> saved = contents(player);
        boolean[] loading = {true};
        SimpleContainer container = new SimpleContainer(SIZE) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                if (!isAccessory(stack)) return false;
                for (int i = 0; i < getContainerSize(); i++) {
                    if (i != slot && getItem(i).is(stack.getItem())) return false;
                }
                return true;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (!loading[0]) {
                    NonNullList<ItemStack> current = contents(player);
                    for (int i = 0; i < SIZE; i++) current.set(i, getItem(i).copy());
                }
            }
        };
        for (int i = 0; i < SIZE; i++) container.setItem(i, saved.get(i).copy());
        loading[0] = false;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, owner) ->
                        new ChestMenu(MenuType.GENERIC_9x4, id, inventory, container, 4),
                Component.literal("Accessory Bag")));
    }

    public static void save(ServerPlayer player, ValueOutput output) {
        try {
            String json = STACK_LIST.encodeStart(
                    player.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                    List.copyOf(contents(player))).getOrThrow().toString();
            output.putString("hypixel_accessory_bag", json);
        } catch (Exception exception) {
            HypixelCustomRecipe.LOGGER.error("Could not save accessory bag for {}", player.getGameProfile().name(), exception);
        }
    }

    public static void load(ServerPlayer player, ValueInput input) {
        String json = input.getStringOr("hypixel_accessory_bag", "");
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (!json.isEmpty()) {
            try {
                List<ItemStack> decoded = STACK_LIST.parse(
                        player.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                        JsonParser.parseString(json)).getOrThrow();
                java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
                for (int i = 0; i < Math.min(SIZE, decoded.size()); i++) {
                    ItemStack stack = decoded.get(i);
                    if (isAccessory(stack) && seen.add(stack.getItem())) {
                        items.set(i, stack.copyWithCount(1));
                    }
                }
            } catch (Exception exception) {
                HypixelCustomRecipe.LOGGER.error("Could not load accessory bag for {}", player.getGameProfile().name(), exception);
            }
        }
        CONTENTS.put(player.getUUID(), items);
    }
}
