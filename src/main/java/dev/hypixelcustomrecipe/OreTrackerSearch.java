package dev.hypixelcustomrecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Incremental nearest-ore search. The datapack's scan was split over many
 * functions; keep the same 32-block spherical search without freezing the
 * server for a large synchronous cube scan.
 */
public final class OreTrackerSearch {
    private static final int RADIUS = 32;
    private static final int POSITIONS_PER_TICK = 5000;
    private static final List<Offset> OFFSETS = createOffsets();
    private static final Map<UUID, Search> ACTIVE = new ConcurrentHashMap<>();

    private OreTrackerSearch() {}

    public static void start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), new Search((ServerLevel) player.level(), player.blockPosition().immutable()));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Searching within 32 blocks in this dimension..."));
    }

    public static void tick(ServerPlayer player) {
        Search search = ACTIVE.get(player.getUUID());
        if (search == null) return;
        if ((ServerLevel) player.level() != search.level) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        int end = Math.min(search.cursor + POSITIONS_PER_TICK, OFFSETS.size());
        while (search.cursor < end) {
            Offset offset = OFFSETS.get(search.cursor++);
            BlockPos pos = search.origin.offset(offset.x, offset.y, offset.z);
            if (!search.level.hasChunkAt(pos)) continue;
            var block = search.level.getBlockState(pos).getBlock();
            if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                    || block == Blocks.ANCIENT_DEBRIS) {
                ACTIVE.remove(player.getUUID());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Nearest ore found at: X: " + pos.getX()
                                + "  Y: " + pos.getY() + "  Z: " + pos.getZ()));
                search.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        8, 0.25, 0.25, 0.25, 0.02);
                return;
            }
        }

        if (search.cursor >= OFFSETS.size()) {
            ACTIVE.remove(player.getUUID());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "No diamond ore or ancient debris found within 32 blocks in this dimension."));
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static List<Offset> createOffsets() {
        List<Offset> offsets = new ArrayList<>();
        int radiusSquared = RADIUS * RADIUS;
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    int distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared <= radiusSquared) {
                        offsets.add(new Offset(x, y, z, distanceSquared));
                    }
                }
            }
        }
        offsets.sort(Comparator.comparingInt(Offset::distanceSquared));
        return List.copyOf(offsets);
    }

    private record Offset(int x, int y, int z, int distanceSquared) {}
    private static final class Search {
        private final ServerLevel level;
        private final BlockPos origin;
        private int cursor;

        private Search(ServerLevel level, BlockPos origin) {
            this.level = level;
            this.origin = origin;
        }
    }
}
