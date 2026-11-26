package dev.hypixelcustomrecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Map;

/**
 * Weapon stat table migrated from the datapack's weapon item modifiers.
 * Strength values are stored as whole points, not decimal multipliers.
 */
public final class VanillaWeaponStats {
    private static final Map<String, WeaponStats> STATS = Map.ofEntries(
            Map.entry("wooden_sword", new WeaponStats(7, 1)),
            Map.entry("stone_sword", new WeaponStats(8, 2)),
            Map.entry("copper_sword", new WeaponStats(50, 4)),
            Map.entry("iron_sword", new WeaponStats(55, 6)),
            Map.entry("golden_sword", new WeaponStats(50, 8)),
            Map.entry("diamond_sword", new WeaponStats(70, 8)),
            Map.entry("netherite_sword", new WeaponStats(85, 11)),
            Map.entry("aote", new WeaponStats(500, 100)),
            Map.entry("aotv", new WeaponStats(600, 100)),
            Map.entry("aotd", new WeaponStats(1125, 100)),
            Map.entry("astraea", new WeaponStats(1350, 150, 0, 0, 50, 250)),
            Map.entry("hyperion", new WeaponStats(1300, 150, 0, 0, 350)),
            Map.entry("zombie_sword", new WeaponStats(500, 50, 0, 0, 50)),
            Map.entry("atomsplit_katana", new WeaponStats(1525, 100, 0, 50, 300)),
            Map.entry("livid_dagger", new WeaponStats(1050, 60, 100, 50, 0)),
            Map.entry("dark_claymore", new WeaponStats(2500, 100, 0, 100, 0)),
            Map.entry("halberd_of_the_shredded", new WeaponStats(700, 115, 0, 0, 0)),
            // Hunter Knife: final Damage is stored directly.
            Map.entry("hunter_knife", new WeaponStats(20, 7)),
            // Raider Axe: final Damage is stored directly.
            Map.entry("raider_axe", new WeaponStats(35, 8)),
            // Vengence: final Damage is stored directly.
            Map.entry("vengence", new WeaponStats(50, 25, -100, 0, 0)),

            Map.entry("wooden_axe", new WeaponStats(8, 0)),
            Map.entry("stone_axe", new WeaponStats(10, 1)),
            Map.entry("copper_axe", new WeaponStats(11, 2)),
            Map.entry("iron_axe", new WeaponStats(14, 2)),
            Map.entry("golden_axe", new WeaponStats(12, 6)),
            Map.entry("diamond_axe", new WeaponStats(17, 4)),
            Map.entry("netherite_axe", new WeaponStats(19, 6))
    );

    /*
     * A tiny server-side hit-registration value. LivingEntityMixin replaces it
     * with the calculated SkyBlock damage before health is reduced.
     */
    public static final ItemAttributeModifiers MELEE_HIT_PLACEHOLDER =
            ItemAttributeModifiers.builder().add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            Identifier.fromNamespaceAndPath("hypixelcustomrecipe", "melee_hit_placeholder"),
                            0.01,
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            ).build();

    private VanillaWeaponStats() {
    }

    public static WeaponStats get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return WeaponStats.NONE;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        WeaponStats weapon = STATS.getOrDefault(id.getPath(), WeaponStats.NONE);
        // Damage values are stored directly as their final intended values.
        return weapon;
    }

    public static SkyblockStats applyHeldWeapon(SkyblockStats armorStats, ItemStack stack) {
        WeaponStats weapon = get(stack);
        return armorStats.withWeaponStats(
                armorStats.weaponDamage() + weapon.damage(),
                weapon.strength(),
                weapon.critChance(),
                weapon.critDamage(),
                weapon.intelligence(),
                weapon.defense()
        );
    }

    public record WeaponStats(int damage, int strength, int critChance, int critDamage, int intelligence, int defense) {
        public WeaponStats(int damage, int strength, int critChance, int critDamage, int intelligence) {
            this(damage, strength, critChance, critDamage, intelligence, 0);
        }

        public WeaponStats(int damage, int strength) {
            this(damage, strength, 0, 0, 0, 0);
        }

        public static final WeaponStats NONE = new WeaponStats(0, 0, 0, 0, 0, 0);
    }
}
