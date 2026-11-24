package dev.hypixelcustomrecipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server-side mana and per-ability cooldown state for one player.
 */
public final class SkyblockManaState {
    private int currentMana = 500;
    private int maximumMana = 500;
    private int regenerationPerSecond = 40;
    private int regenerationDelayTicks;
    private int regenerationProgressTicks;
    private final Map<String, Integer> abilityCooldowns = new HashMap<>();

    public int currentMana() { return currentMana; }
    public int maximumMana() { return maximumMana; }
    public int regenerationPerSecond() { return regenerationPerSecond; }

    /** Compatibility accessor: returns the longest active ability cooldown. */
    public int abilityCooldownTicks() {
        return abilityCooldowns.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public void configure(int maximumMana, int regenerationPerSecond) {
        this.maximumMana = Math.max(1, maximumMana);
        this.regenerationPerSecond = Math.max(0, regenerationPerSecond);
        this.currentMana = Math.min(currentMana, this.maximumMana);
    }

    public void setCurrentMana(int amount) {
        currentMana = Math.max(0, Math.min(amount, maximumMana));
    }

    public boolean spend(int cost) {
        if (cost < 0 || currentMana < cost) return false;
        currentMana -= cost;
        return true;
    }

    public void restore(int amount) {
        if (amount > 0) currentMana = Math.min(maximumMana, currentMana + amount);
    }

    public void setRegenerationDelay(int ticks) {
        regenerationDelayTicks = Math.max(0, ticks);
        regenerationProgressTicks = 0;
    }

    /** Legacy shared cooldown API, retained for callers that need it. */
    public void startCooldown(int ticks) {
        startCooldown("default", ticks);
    }

    public boolean isOnCooldown() {
        return abilityCooldownTicks() > 0;
    }

    public boolean isOnCooldown(String abilityId) {
        return cooldownTicks(abilityId) > 0;
    }

    public int cooldownTicks(String abilityId) {
        return abilityCooldowns.getOrDefault(abilityId, 0);
    }

    public void startCooldown(String abilityId, int ticks) {
        if (ticks <= 0) return;
        abilityCooldowns.merge(abilityId, ticks, Math::max);
    }

    public void tick() {
        Iterator<Map.Entry<String, Integer>> iterator = abilityCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) iterator.remove();
            else entry.setValue(remaining);
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
