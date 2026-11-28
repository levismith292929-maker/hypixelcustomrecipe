package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared 100x100 bedrock arena used by every Wither Trial boss. */
public final class BossArena {
    private static final int HALF_SIZE = 50;
    private static final int WALL_HEIGHT = 12;

    private static ServerLevel level;
    private static BlockPos center;
    private static final Map<BlockPos, BlockState> ORIGINAL_BLOCKS = new LinkedHashMap<>();

    private BossArena() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(BossArena::tick);
    }

    /**
     * Creates the shared arena once. Later boss phases reuse the same center and
     * original-block snapshot so the arena remains unchanged through the full trial.
     */
    public static BlockPos ensure(ServerLevel world, BlockPos requestedCenter) {
        if (level == world && center != null) return center;
        if (level != null) restore();

        level = world;
        center = requestedCenter.immutable();
        build();
        return center;
    }

    public static BlockPos getCenter(ServerLevel world, BlockPos fallback) {
        return level == world && center != null ? center : fallback.immutable();
    }

    public static boolean canPlaceBlock(ServerPlayer player, BlockPos pos) {
        if (level == null || player.level() != level || center == null) return true;
        return !insideBuildArea(pos);
    }

    private static boolean insideBuildArea(BlockPos pos) {
        return pos.getX() >= center.getX() - HALF_SIZE
                && pos.getX() < center.getX() + HALF_SIZE
                && pos.getZ() >= center.getZ() - HALF_SIZE
                && pos.getZ() < center.getZ() + HALF_SIZE
                && pos.getY() > center.getY();
    }

    private static boolean insideFootprint(ServerPlayer player) {
        return player.getX() >= center.getX() - HALF_SIZE
                && player.getX() < center.getX() + HALF_SIZE
                && player.getZ() >= center.getZ() - HALF_SIZE
                && player.getZ() < center.getZ() + HALF_SIZE
                && player.getY() > center.getY();
    }

    private static void tick(MinecraftServer server) {
        if (level == null || center == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && player.isFallFlying() && insideFootprint(player)) {
                player.stopFallFlying();
            }
        }
    }

    private static void build() {
        ORIGINAL_BLOCKS.clear();
        int minX = center.getX() - HALF_SIZE;
        int maxX = center.getX() + HALF_SIZE - 1;
        int minZ = center.getZ() - HALF_SIZE;
        int maxZ = center.getZ() + HALF_SIZE - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                saveAndPlaceBedrock(new BlockPos(x, center.getY(), z));
                boolean boundary = x == minX || x == maxX || z == minZ || z == maxZ;
                for (int y = 1; y <= WALL_HEIGHT; y++) {
                    BlockPos pos = new BlockPos(x, center.getY() + y, z);
                    if (boundary) {
                        saveAndPlaceBedrock(pos);
                    } else {
                        BlockPos immutable = pos.immutable();
                        BlockState original = level.getBlockState(immutable);
                        if (!original.isAir()) {
                            ORIGINAL_BLOCKS.putIfAbsent(immutable, original);
                            level.setBlock(immutable, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    private static void saveAndPlaceBedrock(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        ORIGINAL_BLOCKS.putIfAbsent(immutable, level.getBlockState(immutable));
        level.setBlock(immutable, Blocks.BEDROCK.defaultBlockState(), 3);
    }

    /** Restores every block changed by the shared arena. */
    public static void restore() {
        if (level == null) return;
        for (Map.Entry<BlockPos, BlockState> entry : ORIGINAL_BLOCKS.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        ORIGINAL_BLOCKS.clear();
        level = null;
        center = null;
    }
}
