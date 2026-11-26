package dev.hypixelcustomrecipe;

/** Applies permanent SkyBlock level bonuses to the player's complete stat total. */
public final class SkyblockLevelBonuses {
    private SkyblockLevelBonuses() {}

    // Player Level bonuses affect the final combined stats, including armor,
    // accessories, and held equipment.
    public static SkyblockStats apply(SkyblockStats base, int level) {
        int safeLevel = Math.max(0, level);
        double healthDefense = 1.0D + safeLevel * 0.05D;
        double intelligence = 1.0D + safeLevel * 0.06D;
        double strength = 1.0D + safeLevel * 0.04D;
        return new SkyblockStats(
                (int) Math.round(base.health() * healthDefense),
                (int) Math.round(base.defense() * healthDefense),
                (int) Math.round(base.strength() * strength),
                (int) Math.round(base.armorStrength() * strength),
                (int) Math.round(base.weaponStrength() * strength),
                base.critChance() + safeLevel,
                (int) Math.round(base.critDamage() * strength),
                (int) Math.round(base.intelligence() * intelligence),
                base.manaMaximum(),
                base.manaRegeneration(),
                base.weaponDamage()
        );
    }
}