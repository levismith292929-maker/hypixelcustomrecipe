package dev.hypixelcustomrecipe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks Vengence's uncapped kill streak while the weapon is held. */
public final class VengenceHandler {
    private static final Map<UUID, Buff> BUFFS = new HashMap<>();
    private static final String ITEM_ID = "vengence";

    private VengenceHandler() {}

    public static void onMobKill(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!isVengence(held)) return;

        long now = player.level().getGameTime();
        Buff previous = BUFFS.get(player.getUUID());
        int stacks = previous != null && now < previous.expiresAt() ? previous.stacks() + 1 : 1;
        BUFFS.put(player.getUUID(), new Buff(stacks, now + 60L));
    }

    public static SkyblockStats applyActiveBuff(ServerPlayer player, SkyblockStats stats) {
        if (!isVengence(player.getMainHandItem())) return stats;
        Buff buff = BUFFS.get(player.getUUID());
        if (buff == null) return stats;
        long now = player.level().getGameTime();
        if (now >= buff.expiresAt()) {
            BUFFS.remove(player.getUUID());
            return stats;
        }

        double multiplier = Math.pow(1.05, buff.stacks());
        return new SkyblockStats(
                stats.health(), stats.defense(),
                stats.strength(), stats.armorStrength(),
                (int) Math.round(stats.weaponStrength() * multiplier),
                stats.critChance(), stats.critDamage(), stats.intelligence(),
                stats.manaMaximum(), stats.manaRegeneration(),
                stats.weaponDamage() * multiplier
        );
    }

    private static boolean isVengence(ItemStack stack) {
        return !stack.isEmpty()
                && "hypixel".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).getNamespace())
                && ITEM_ID.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).getPath());
    }

    private record Buff(int stacks, long expiresAt) {}
}
