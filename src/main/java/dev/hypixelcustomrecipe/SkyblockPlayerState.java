package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the live per-player SkyBlock state on the logical server. */
public final class SkyblockPlayerState {
    public enum Pet {
        NONE(500, 40, 0.0),
        ENDERMAN(1000, 50, -0.20),
        ENDER_DRAGON(300, 20, 0.15),
        GOLDEN_DRAGON(750, 10, 0.40),
        IRON_GOLEM(600, 30, 0.05);

        private final int manaMaximum;
        private final int manaRegeneration;
        private final double damageBonus;

        Pet(int manaMaximum, int manaRegeneration, double damageBonus) {
            this.manaMaximum = manaMaximum;
            this.manaRegeneration = manaRegeneration;
            this.damageBonus = damageBonus;
        }

        public int manaMaximum() { return manaMaximum; }
        public int manaRegeneration() { return manaRegeneration; }
        public double damageMultiplier() { return 1.0 + damageBonus; }
    }

    private static final Map<UUID, SkyblockPlayerState> PLAYERS = new ConcurrentHashMap<>();
    private final SkyblockManaState mana = new SkyblockManaState();
    private SkyblockStats stats = SkyblockStats.BASE;
    private Pet activePet = Pet.NONE;

    private SkyblockPlayerState() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) get(player).tick(player);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PLAYERS.clear());
    }

    public static SkyblockPlayerState get(Player player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new SkyblockPlayerState());
    }

    public SkyblockStats stats() { return stats; }
    public SkyblockManaState mana() { return mana; }
    public Pet activePet() { return activePet; }

    public void activatePet(ServerPlayer player, Pet pet) {
        if (activePet == Pet.IRON_GOLEM) player.removeEffect(MobEffects.RESISTANCE);
        activePet = pet;
        if (pet == Pet.IRON_GOLEM) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
        }
        mana.configure(pet.manaMaximum() + stats.intelligence(),
                pet.manaRegeneration() + Math.max(0, (stats.intelligence() + 2) / 4));
    }

    private void tick(ServerPlayer player) {
        stats = VanillaWeaponStats.applyHeldWeapon(ModArmorItems.getSkyblockStats(player), player.getMainHandItem());
        mana.configure(activePet.manaMaximum() + stats.intelligence(),
                activePet.manaRegeneration() + Math.max(0, (stats.intelligence() + 2) / 4));
        mana.tick();
    }
}
