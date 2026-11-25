package dev.hypixelcustomrecipe;

/** Applies permanent SkyBlock level bonuses to the combined equipment stats. */
public final class SkyblockLevelBonuses {
    private SkyblockLevelBonuses() {}

    public static double generalMultiplier(int level) { return 1.0D + Math.max(0, level) * 0.10D; }
    public static double vitalityMultiplier(int level) { return 1.0D + Math.max(0, level) * 0.15D; }

    public static int scaleGeneral(int value, int level) {
        return (int) Math.ceil(Math.max(0, value) * generalMultiplier(level));
    }

    public static SkyblockStats apply(SkyblockStats base, int level) {
        int safeLevel = Math.max(0, level);
        double survival = 1.0D + safeLevel * 0.20D;
        double general = generalMultiplier(safeLevel);
        double damage = vitalityMultiplier(safeLevel);
        return new SkyblockStats(
                (int) Math.ceil(base.health() * survival),
                (int) Math.ceil(base.defense() * survival),
                (int) Math.ceil(base.strength() * general),
                (int) Math.ceil(base.armorStrength() * general),
                (int) Math.ceil(base.weaponStrength() * general),
                base.critChance() + safeLevel,
                base.critDamage() + safeLevel * 10,
                (int) Math.ceil(base.intelligence() * general),
                base.manaMaximum(),
                (int) Math.ceil(base.manaRegeneration() * general),
                Math.ceil(base.weaponDamage() * damage)
        );
    }
}
