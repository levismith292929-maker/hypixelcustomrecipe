package dev.hypixelcustomrecipe;

import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player eight-gem storage and the Gem Tank's nine-cell display (eight usable slots). */
public final class GemTankMenu {
    private static final int SIZE = 8;
    private static final Map<UUID, NonNullList<ItemStack>> CONTENTS = new ConcurrentHashMap<>();
    private static final Codec<Map<String, ItemStack>> STACK_MAP = Codec.unboundedMap(Codec.STRING, ItemStack.CODEC);

    private GemTankMenu() {}

    private static NonNullList<ItemStack> contents(ServerPlayer player) {
        return CONTENTS.computeIfAbsent(player.getUUID(), ignored ->
                NonNullList.withSize(SIZE, ItemStack.EMPTY));
    }

    public static void clear(ServerPlayer player) {
        NonNullList<ItemStack> items = contents(player);
        for (int i = 0; i < SIZE; i++) items.set(i, ItemStack.EMPTY);
    }

    public static boolean isGemstone(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return gemstoneType(stack) >= 0;
    }

    private static int gemstoneType(ItemStack stack) {
        if (stack.is(GemstoneItems.ROUGH_RUBY_GEMSTONE) || stack.is(GemstoneItems.FLAWED_RUBY_GEMSTONE)
                || stack.is(GemstoneItems.FINE_RUBY_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_RUBY_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_RUBY_GEMSTONE) || stack.is(GemstoneCoreItems.RUBY_GEMSTONE_CORE)) return 0;
        if (stack.is(GemstoneItems.ROUGH_AMETHYST_GEMSTONE) || stack.is(GemstoneItems.FLAWED_AMETHYST_GEMSTONE)
                || stack.is(GemstoneItems.FINE_AMETHYST_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_AMETHYST_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_AMETHYST_GEMSTONE) || stack.is(GemstoneCoreItems.AMETHYST_GEMSTONE_CORE)) return 1;
        if (stack.is(GemstoneItems.ROUGH_AMBER_GEMSTONE) || stack.is(GemstoneItems.FLAWED_AMBER_GEMSTONE)
                || stack.is(GemstoneItems.FINE_AMBER_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_AMBER_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_AMBER_GEMSTONE) || stack.is(GemstoneCoreItems.AMBER_GEMSTONE_CORE)) return 2;
        if (stack.is(GemstoneItems.ROUGH_TOPAZ_GEMSTONE) || stack.is(GemstoneItems.FLAWED_TOPAZ_GEMSTONE)
                || stack.is(GemstoneItems.FINE_TOPAZ_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_TOPAZ_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_TOPAZ_GEMSTONE) || stack.is(GemstoneCoreItems.TOPAZ_GEMSTONE_CORE)) return 3;
        if (stack.is(GemstoneItems.ROUGH_SAPPHIRE_GEMSTONE) || stack.is(GemstoneItems.FLAWED_SAPPHIRE_GEMSTONE)
                || stack.is(GemstoneItems.FINE_SAPPHIRE_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_SAPPHIRE_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_SAPPHIRE_GEMSTONE) || stack.is(GemstoneCoreItems.SAPPHIRE_GEMSTONE_CORE)) return 4;
        if (stack.is(GemstoneItems.ROUGH_JADE_GEMSTONE) || stack.is(GemstoneItems.FLAWED_JADE_GEMSTONE)
                || stack.is(GemstoneItems.FINE_JADE_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_JADE_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_JADE_GEMSTONE) || stack.is(GemstoneCoreItems.JADE_GEMSTONE_CORE)) return 5;
        if (stack.is(GemstoneItems.ROUGH_OPAL_GEMSTONE) || stack.is(GemstoneItems.FLAWED_OPAL_GEMSTONE)
                || stack.is(GemstoneItems.FINE_OPAL_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_OPAL_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_OPAL_GEMSTONE) || stack.is(GemstoneCoreItems.OPAL_GEMSTONE_CORE)) return 6;
        if (stack.is(GemstoneItems.ROUGH_JASPER_GEMSTONE) || stack.is(GemstoneItems.FLAWED_JASPER_GEMSTONE)
                || stack.is(GemstoneItems.FINE_JASPER_GEMSTONE) || stack.is(GemstoneItems.FLAWLESS_JASPER_GEMSTONE)
                || stack.is(GemstoneItems.PERFECT_JASPER_GEMSTONE) || stack.is(GemstoneCoreItems.JASPER_GEMSTONE_CORE)) return 7;
        return -1;
    }

    private static double qualityPercent(ItemStack stack) {
        if (stack.is(GemstoneCoreItems.RUBY_GEMSTONE_CORE) || stack.is(GemstoneCoreItems.AMETHYST_GEMSTONE_CORE)
                || stack.is(GemstoneCoreItems.AMBER_GEMSTONE_CORE) || stack.is(GemstoneCoreItems.TOPAZ_GEMSTONE_CORE)
                || stack.is(GemstoneCoreItems.SAPPHIRE_GEMSTONE_CORE) || stack.is(GemstoneCoreItems.JADE_GEMSTONE_CORE)
                || stack.is(GemstoneCoreItems.OPAL_GEMSTONE_CORE) || stack.is(GemstoneCoreItems.JASPER_GEMSTONE_CORE)) return 25.0;
        if (stack.getItem() == GemstoneItems.PERFECT_RUBY_GEMSTONE || stack.getItem() == GemstoneItems.PERFECT_AMETHYST_GEMSTONE
                || stack.getItem() == GemstoneItems.PERFECT_AMBER_GEMSTONE || stack.getItem() == GemstoneItems.PERFECT_TOPAZ_GEMSTONE
                || stack.getItem() == GemstoneItems.PERFECT_SAPPHIRE_GEMSTONE || stack.getItem() == GemstoneItems.PERFECT_JADE_GEMSTONE
                || stack.getItem() == GemstoneItems.PERFECT_OPAL_GEMSTONE || stack.getItem() == GemstoneItems.PERFECT_JASPER_GEMSTONE) return 12.0;
        if (stack.getItem() == GemstoneItems.FLAWLESS_RUBY_GEMSTONE || stack.getItem() == GemstoneItems.FLAWLESS_AMETHYST_GEMSTONE
                || stack.getItem() == GemstoneItems.FLAWLESS_AMBER_GEMSTONE || stack.getItem() == GemstoneItems.FLAWLESS_TOPAZ_GEMSTONE
                || stack.getItem() == GemstoneItems.FLAWLESS_SAPPHIRE_GEMSTONE || stack.getItem() == GemstoneItems.FLAWLESS_JADE_GEMSTONE
                || stack.getItem() == GemstoneItems.FLAWLESS_OPAL_GEMSTONE || stack.getItem() == GemstoneItems.FLAWLESS_JASPER_GEMSTONE) return 8.0;
        if (stack.getItem() == GemstoneItems.FINE_RUBY_GEMSTONE || stack.getItem() == GemstoneItems.FINE_AMETHYST_GEMSTONE
                || stack.getItem() == GemstoneItems.FINE_AMBER_GEMSTONE || stack.getItem() == GemstoneItems.FINE_TOPAZ_GEMSTONE
                || stack.getItem() == GemstoneItems.FINE_SAPPHIRE_GEMSTONE || stack.getItem() == GemstoneItems.FINE_JADE_GEMSTONE
                || stack.getItem() == GemstoneItems.FINE_OPAL_GEMSTONE || stack.getItem() == GemstoneItems.FINE_JASPER_GEMSTONE) return 5.0;
        if (stack.getItem() == GemstoneItems.FLAWED_RUBY_GEMSTONE || stack.getItem() == GemstoneItems.FLAWED_AMETHYST_GEMSTONE
                || stack.getItem() == GemstoneItems.FLAWED_AMBER_GEMSTONE || stack.getItem() == GemstoneItems.FLAWED_TOPAZ_GEMSTONE
                || stack.getItem() == GemstoneItems.FLAWED_SAPPHIRE_GEMSTONE || stack.getItem() == GemstoneItems.FLAWED_JADE_GEMSTONE
                || stack.getItem() == GemstoneItems.FLAWED_OPAL_GEMSTONE || stack.getItem() == GemstoneItems.FLAWED_JASPER_GEMSTONE) return 3.0;
        return stack.isEmpty() ? 0.0 : 1.0;
    }

    public static double auraPercent(ServerPlayer player, int gemstoneIndex) {
        NonNullList<ItemStack> items = contents(player);
        return gemstoneIndex >= 0 && gemstoneIndex < SIZE ? qualityPercent(items.get(gemstoneIndex)) : 0.0;
    }

    public static void open(ServerPlayer player) {
        NonNullList<ItemStack> saved = contents(player);
        boolean[] loading = {true};
        SimpleContainer container = new SimpleContainer(9) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                if (slot >= SIZE || !isGemstone(stack)) return false;
                int type = gemstoneType(stack);
                for (int i = 0; i < SIZE; i++) {
                    if (i != slot && !getItem(i).isEmpty() && gemstoneType(getItem(i)) == type) return false;
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
        container.setItem(8, new ItemStack(Items.BARRIER));
        loading[0] = false;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, owner) -> {
                    ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x1, id, inventory, container, 1);
                    // The ninth slot is a decorative barrier, never a player-accessible slot.
                    menu.slots.set(8, new net.minecraft.world.inventory.Slot(container, 8, 8 + 8 * 18, 18) {
                        @Override
                        public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                            return false;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    });
                    return menu;
                },
                Component.literal("Gem Tank")));
    }

    public static void save(ServerPlayer player, ValueOutput output) {
        try {
            // Empty ItemStack entries encode as minecraft:air with count 0, which
            // the 26.2 item codec rejects. Store only occupied slots, keyed by index.
            Map<String, ItemStack> occupied = new java.util.TreeMap<>();
            NonNullList<ItemStack> items = contents(player);
            for (int i = 0; i < SIZE; i++) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) occupied.put(Integer.toString(i), stack.copy());
            }
            String json = STACK_MAP.encodeStart(
                    player.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                    occupied).getOrThrow().toString();
            output.putString("hypixel_gem_tank", json);
        } catch (Exception exception) {
            HypixelCustomRecipe.LOGGER.error("Could not save Gem Tank for {}", player.getGameProfile().name(), exception);
        }
    }

    public static void load(ServerPlayer player, ValueInput input) {
        String json = input.getStringOr("hypixel_gem_tank", "");
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (!json.isEmpty()) {
            try {
                JsonElement root = JsonParser.parseString(json);
                var ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
                java.util.Set<net.minecraft.world.item.Item> seenItems = new java.util.HashSet<>();
                boolean[] usedGemTypes = new boolean[SIZE];

                // New format: sparse object, so empty slots are never serialized.
                if (root.isJsonObject()) {
                    Map<String, ItemStack> decoded = STACK_MAP.parse(ops, root).getOrThrow();
                    for (Map.Entry<String, ItemStack> entry : decoded.entrySet()) {
                        int slot;
                        try { slot = Integer.parseInt(entry.getKey()); }
                        catch (NumberFormatException ignored) { continue; }
                        if (slot < 0 || slot >= SIZE) continue;
                        ItemStack stack = entry.getValue();
                        if (stack.isEmpty()) continue;
                        boolean allowed = gemstoneType(stack) >= 0 && !usedGemTypes[gemstoneType(stack)];
                        if (allowed) {
                            items.set(slot, stack.copyWithCount(1));
                            usedGemTypes[gemstoneType(stack)] = true;
                        }
                    }
                } else if (root.isJsonArray()) {
                    // Backward compatibility: read the old positional array one slot
                    // at a time. A malformed/empty entry no longer discards the other slots.
                    JsonArray legacy = root.getAsJsonArray();
                    for (int i = 0; i < Math.min(SIZE, legacy.size()); i++) {
                        JsonElement element = legacy.get(i);
                        if (element == null || element.isJsonNull()) continue;
                        final int slotIndex = i;
                        ItemStack stack = ItemStack.CODEC.parse(ops, element)
                                .resultOrPartial(message -> HypixelCustomRecipe.LOGGER.warn(
                                        "Skipping invalid legacy Gem Tank slot {} for {}: {}", slotIndex,
                                        player.getGameProfile().name(), message))
                                .orElse(ItemStack.EMPTY);
                        if (stack.isEmpty()) continue;
                        boolean allowed = gemstoneType(stack) >= 0 && !usedGemTypes[gemstoneType(stack)];
                        if (allowed) {
                            items.set(i, stack.copyWithCount(1));
                            usedGemTypes[gemstoneType(stack)] = true;
                        }
                    }
                }
            } catch (Exception exception) {
                HypixelCustomRecipe.LOGGER.error("Could not load Gem Tank for {}", player.getGameProfile().name(), exception);
            }
        }
        CONTENTS.put(player.getUUID(), items);
    }

    public static double qualityPercentFor(ServerPlayer player, int type) {
        for (ItemStack stack : contents(player)) {
            if (!stack.isEmpty() && gemstoneType(stack) == type) return qualityPercent(stack);
        }
        return 0.0;
    }
}
