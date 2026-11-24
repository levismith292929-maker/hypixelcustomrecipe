package dev.hypixelcustomrecipe;

/**
 * Server-side mana and ability-cooldown state for one player.
 *
 * The owning player-state manager will retain one instance per player.
 * Cooldowns and regeneration are advanced in game ticks (20 ticks/second).
 */
public final class SkyblockManaState {
    private int currentMana = 500;
    private int maximumMana = 500;
    private int regenerationPerSecond = 40;
    private int regenerationDelayTicks;
    private int regenerationProgressTicks;
    private int abilityCooldownTicks;

    public int currentMana() {
        return currentMana;
    }

    public int maximumMana() {
        return maximumMana;
    }

    public int regenerationPerSecond() {
        return regenerationPerSecond;
    }

    public int abilityCooldownTicks() {
        return abilityCooldownTicks;
    }

    public void configure(int maximumMana, int regenerationPerSecond) {
        this.maximumMana = Math.max(1, maximumMana);
        this.regenerationPerSecond = Math.max(0, regenerationPerSecond);
        this.currentMana = Math.min(currentMana, this.maximumMana);
    }

    public void setCurrentMana(int amount) {
        currentMana = Math.max(0, Math.min(amount, maximumMana));
    }

    /** Returns false and changes nothing when the player cannot afford the cost. */
    public boolean spend(int cost) {
        if (cost < 0 || currentMana < cost) {
            return false;
        }
        currentMana -= cost;
        return true;
    }

    public void restore(int amount) {
        if (amount > 0) {
            currentMana = Math.min(maximumMana, currentMana + amount);
        }
    }

    public void setRegenerationDelay(int ticks) {
        regenerationDelayTicks = Math.max(0, ticks);
        regenerationProgressTicks = 0;
    }

    public void startCooldown(int ticks) {
        abilityCooldownTicks = Math.max(abilityCooldownTicks, Math.max(0, ticks));
    }

    public boolean isOnCooldown() {
        return abilityCooldownTicks > 0;
    }

    /** Advance this player's mana and cooldown timers by one server tick. */
    public void tick() {
        if (abilityCooldownTicks > 0) {
            abilityCooldownTicks--;
        }
        if (regenerationDelayTicks > 0) {
            regenerationDelayTicks--;
            return;
        }
        if (currentMana >= maximumMana || regenerationPerSecond <= 0) {
            regenerationProgressTicks = 0;
            return;
        }

        regenerationProgressTicks++;
        if (regenerationProgressTicks >= 20) {
            currentMana = Math.min(maximumMana, currentMana + regenerationPerSecond);
            regenerationProgressTicks = 0;
        }
    }
}
