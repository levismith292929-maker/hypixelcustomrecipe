package dev.hypixelcustomrecipe;

/**
 * Immutable snapshot of the custom SkyBlock-style combat stats.
 *
 * This is the common data model for the migrated stat calculator, HUD,
 * Stat Viewer, and ability damage calculations. Values are expressed in
 * whole stat points (for example, 25 means 25% Crit Chance). Health is stored
 * in tenths to match the datapack's custom Health pool.
 */
public record SkyblockStats(
        int health,
        int defense,
        int strength,
        int armorStrength,
        int weaponStrength,
        int critChance,
        int critDamage,
        int intelligence,
        int manaMaximum,
        int manaRegeneration,
        double weaponDamage
) {
    public static final SkyblockStats BASE = new SkyblockStats(
            1000, 0, 0, 0, 0, 0, 0, 0, 500, 40, 1.0
    );

    public SkyblockStats {
        health = Math.max(1, health);
        defense = Math.max(0, defense);
        // Negative Crit Chance is valid for weapons such as Vengence.
        critDamage = Math.max(0, critDamage);
        manaMaximum = Math.max(1, manaMaximum);
        manaRegeneration = Math.max(0, manaRegeneration);
        weaponDamage = Math.max(0.0, weaponDamage);
    }

    /**
     * Hypixel-style melee scaling: all Strength sources contribute at 1% per
     * point. Weapon Strength is kept separate in the stat display, but it
     * contributes to the same melee multiplier.
     */
    public double strengthMultiplier() {
        return 1.0 + (strength + armorStrength + weaponStrength) / 100.0;
    }

    /** Expected critical multiplier, before checking the random crit roll. */
    public double criticalMultiplier() {
        return 3.0 + critDamage / 50.0;
    }

    /**
     * Defense uses the tuned diminishing-returns curve. At 13,000 Defense,
     * damage reduction is 30%; additional Defense continues scaling smoothly.
     */
    public double damageReduction() {
        return damageReduction(0);
    }

    /** True Defense raises the maximum reduction cap by one percentage point per point. */
    public double damageReduction(int trueDefense) {
        double cap = Math.min(1.0D, 0.60D + Math.max(0, trueDefense) / 100.0D);
        return Math.min(cap, defense / (defense + 30333.3333333333D));
    }

    public double applyDefense(double incomingDamage) {
        return applyDefense(incomingDamage, 0);
    }

    public double applyDefense(double incomingDamage, int trueDefense) {
        return Math.max(0.0, incomingDamage) * (1.0 - damageReduction(trueDefense));
    }

    public double calculateWeaponDamage(boolean critical) {
        // The standard melee formula includes 5 base damage before weapon
        // damage and Strength scaling.
        double damage = (5.0 + weaponDamage) * strengthMultiplier();
        return critical ? damage * criticalMultiplier() : damage;
    }

    public SkyblockStats add(
            int health,
            int defense,
            int strength,
            int armorStrength,
            int critChance,
            int critDamage,
            int intelligence,
            int manaMaximum,
            int manaRegeneration
    ) {
        return new SkyblockStats(
                this.health + health,
                this.defense + defense,
                this.strength + strength,
                this.armorStrength + armorStrength,
                this.weaponStrength,
                this.critChance + critChance,
                this.critDamage + critDamage,
                this.intelligence + intelligence,
                this.manaMaximum + manaMaximum,
                this.manaRegeneration + manaRegeneration,
                this.weaponDamage
        );
    }

    public SkyblockStats withWeaponStats(double damage, int strength) {
        return withWeaponStats(damage, strength, 0, 0, 0);
    }

    public SkyblockStats withWeaponStats(double damage, int strength,
                                         int weaponCritChance, int weaponCritDamage,
                                         int weaponIntelligence) {
        return withWeaponStats(damage, strength, weaponCritChance, weaponCritDamage, weaponIntelligence, 0);
    }

    public SkyblockStats withWeaponStats(double damage, int strength,
                                         int weaponCritChance, int weaponCritDamage,
                                         int weaponIntelligence, int weaponDefense) {
        return new SkyblockStats(
                health, defense + Math.max(0, weaponDefense), this.strength, armorStrength, Math.max(0, strength),
                critChance + weaponCritChance,
                critDamage + Math.max(0, weaponCritDamage),
                intelligence + Math.max(0, weaponIntelligence),
                manaMaximum, manaRegeneration, Math.max(0.0, damage)
        );
    }

    public SkyblockStats withWeaponDamage(double damage) {
        return new SkyblockStats(
                health, defense, strength, armorStrength, weaponStrength, critChance,
                critDamage, intelligence, manaMaximum, manaRegeneration, damage
        );
    }
}
