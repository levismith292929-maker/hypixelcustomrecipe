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
        critChance = Math.max(0, critChance);
        critDamage = Math.max(0, critDamage);
        manaMaximum = Math.max(1, manaMaximum);
        manaRegeneration = Math.max(0, manaRegeneration);
        weaponDamage = Math.max(0.0, weaponDamage);
    }

    /** Strength and armor Strength jointly scale the held weapon's damage. */
    public double strengthMultiplier() {
        return 1.0 + (strength + armorStrength) / 200.0 + weaponStrength / 100.0;
    }

    /** Expected critical multiplier, before checking the random crit roll. */
    public double criticalMultiplier() {
        return 3.0 + critDamage / 50.0;
    }

    /**
     * Matches the datapack's integer scoreboard calculation:
     * reductionPercent = min(100, (Defense + 50) / 100).
     */
    public double damageReduction() {
        int reductionPercent = Math.min(100, (defense + 50) / 100);
        return reductionPercent / 100.0;
    }

    public double applyDefense(double incomingDamage) {
        return Math.max(0.0, incomingDamage) * (1.0 - damageReduction());
    }

    public double calculateWeaponDamage(boolean critical) {
        double damage = weaponDamage * strengthMultiplier();
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
                critChance + Math.max(0, weaponCritChance),
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
