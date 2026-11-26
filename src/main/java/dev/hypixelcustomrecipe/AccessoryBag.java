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
                || stack.is(GemstoneAccessoryItems.NATURE_GEM));
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

        return new SkyblockStats(
                scale(original.health(), health), scale(original.defense(), defense),
                scale(original.strength(), strength), scale(original.armorStrength(), strength),
                scale(original.weaponStrength(), strength), scale(original.critChance(), critChance),
                scale(original.critDamage(), critDamage), scale(original.intelligence(), intelligence),
                scale(original.manaMaximum(), mana), scale(original.manaRegeneration(), manaRegen),
                original.weaponDamage() * weaponDamage
        );
    }

    /** Total additive Gemstone Fortune percentage from accessories and the Gem Tank. */
    public static double gemstoneFortuneBonusPercent(ServerPlayer player) {
        double bonus = 0.0;
        if (has(player, GemstoneAccessoryItems.AMETHYST_MASK)) bonus += 10.0;
        if (has(player, GemstoneAccessoryItems.NATURE_GEM)) bonus += 2.0;
        if (has(player, GemstoneAccessoryItems.JASPER_MASK)) bonus += 4.0;
        bonus += GemTankMenu.qualityPercentFor(player, 1);
        bonus += GemTankMenu.qualityPercentFor(player, 7);
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
