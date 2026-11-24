package dev.hypixelcustomrecipe;

/**
 * Immutable snapshot of the custom SkyBlock-style combat stats.
 *
 * This is the common data model for the migrated stat calculator, HUD,
 * Stat Viewer, and ability damage calculations. Values are expressed in
 * whole stat points (for example, 25 means 25% Crit Chance).
 */
public record SkyblockStats(
        int health,
        int defense,
        int strength,
        int armorStrength,
        int critChance,
        int critDamage,
        int intelligence,
        int manaMaximum,
        int manaRegeneration,
        double weaponDamage
) {
    public static final SkyblockStats BASE = new SkyblockStats(
            100, 0, 0, 0, 5, 0, 0, 500, 40, 1.0
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
        return 1.0 + (strength + armorStrength) / 100.0;
    }

    /** Expected critical multiplier, before checking the random crit roll. */
    public double criticalMultiplier() {
        return 3.0 + critDamage / 50.0;
    }

    /** Defense-based fraction of incoming damage that is prevented. */
    public double damageReduction() {
        return Math.min(1.0, defense / (defense + 50.0));
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
                this.critChance + critChance,
                this.critDamage + critDamage,
                this.intelligence + intelligence,
                this.manaMaximum + manaMaximum,
                this.manaRegeneration + manaRegeneration,
                this.weaponDamage
        );
    }

    public SkyblockStats withWeaponDamage(double damage) {
        return new SkyblockStats(
                health, defense, strength, armorStrength, critChance,
                critDamage, intelligence, manaMaximum, manaRegeneration, damage
        );
    }
}
