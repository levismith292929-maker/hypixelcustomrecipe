package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the live per-player SkyBlock state on the logical server.
 *
 * The first runtime pass intentionally keeps state in memory for the server
 * session. Persistent player-data serialization will be added separately.
 */
public final class SkyblockPlayerState {
    private static final Map<UUID, SkyblockPlayerState> PLAYERS = new ConcurrentHashMap<>();

    private final SkyblockManaState mana = new SkyblockManaState();
    private SkyblockStats stats = SkyblockStats.BASE;

    private SkyblockPlayerState() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                get(player).tick(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PLAYERS.clear());
    }

    public static SkyblockPlayerState get(Player player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new SkyblockPlayerState());
    }

    public SkyblockStats stats() {
        return stats;
    }

    public SkyblockManaState mana() {
        return mana;
    }

    private void tick(ServerPlayer player) {
        stats = VanillaWeaponStats.applyHeldWeapon(
                ModArmorItems.getSkyblockStats(player), player.getMainHandItem()
        );
        mana.configure(stats.manaMaximum(), stats.manaRegeneration());
        mana.tick();
    }
}
