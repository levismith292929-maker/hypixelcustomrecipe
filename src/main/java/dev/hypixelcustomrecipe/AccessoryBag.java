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
import net.minecraft.world.item.Items;
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
        return !stack.isEmpty() && stack.is(DatapackItems.ADMIN_RING);
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
                NonNullList<ItemStack> current = contents(player);
                for (int i = 0; i < SIZE; i++) current.set(i, getItem(i).copy());
            }
        };
        for (int i = 0; i < SIZE; i++) container.setItem(i, saved.get(i).copy());
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
                for (int i = 0; i < Math.min(SIZE, decoded.size()); i++) {
                    if (isAccessory(decoded.get(i))) items.set(i, decoded.get(i));
                }
            } catch (Exception exception) {
                HypixelCustomRecipe.LOGGER.error("Could not load accessory bag for {}", player.getGameProfile().name(), exception);
            }
        }
        CONTENTS.put(player.getUUID(), items);
    }
}
