package dev.hypixelcustomrecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Weapon stat table migrated from the datapack's weapon item modifiers.
 * Strength values are stored as whole points, not decimal multipliers.
 */
public final class VanillaWeaponStats {
    private static final Map<String, WeaponStats> STATS = Map.ofEntries(
            Map.entry("wooden_sword", new WeaponStats(7, 1)),
            Map.entry("stone_sword", new WeaponStats(8, 2)),
            Map.entry("copper_sword", new WeaponStats(10, 4)),
            Map.entry("iron_sword", new WeaponStats(11, 6)),
            Map.entry("golden_sword", new WeaponStats(10, 8)),
            Map.entry("diamond_sword", new WeaponStats(14, 8)),
            Map.entry("netherite_sword", new WeaponStats(17, 11)),
            Map.entry("wooden_axe", new WeaponStats(8, 0)),
            Map.entry("stone_axe", new WeaponStats(10, 1)),
            Map.entry("copper_axe", new WeaponStats(11, 2)),
            Map.entry("iron_axe", new WeaponStats(14, 2)),
            Map.entry("golden_axe", new WeaponStats(12, 6)),
            Map.entry("diamond_axe", new WeaponStats(17, 4)),
            Map.entry("netherite_axe", new WeaponStats(19, 6))
    );

    private VanillaWeaponStats() {
    }

    public static WeaponStats get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return WeaponStats.NONE;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return STATS.getOrDefault(id.getPath(), WeaponStats.NONE);
    }

    public static SkyblockStats applyHeldWeapon(SkyblockStats armorStats, ItemStack stack) {
        WeaponStats weapon = get(stack);
        return armorStats.withWeaponStats(armorStats.weaponDamage() + weapon.damage(), weapon.strength());
    }

    public record WeaponStats(int damage, int strength) {
        public static final WeaponStats NONE = new WeaponStats(0, 0);
    }
}
