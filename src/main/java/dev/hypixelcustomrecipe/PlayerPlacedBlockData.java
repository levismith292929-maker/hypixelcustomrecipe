package dev.hypixelcustomrecipe;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * World-persistent record of blocks placed by players.
 *
 * One shared Overworld SavedData instance stores positions from every
 * dimension. Lookups use a HashSet, so mining checks do not scan the list.
 */
public final class PlayerPlacedBlockData extends SavedData {
    private static final Codec<PlayerPlacedBlockData> CODEC = Codec.STRING.listOf().xmap(
            PlayerPlacedBlockData::new,
            data -> new ArrayList<>(data.positions)
    );

    private static final SavedDataType<PlayerPlacedBlockData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HypixelCustomRecipe.MOD_ID, "player_placed_blocks"),
            PlayerPlacedBlockData::new,
            CODEC,
            null
    );

    private final Set<String> positions = new HashSet<>();

    private PlayerPlacedBlockData() {}

    private PlayerPlacedBlockData(List<String> savedPositions) {
        positions.addAll(savedPositions);
    }

    public static PlayerPlacedBlockData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean contains(ServerLevel level, BlockPos pos) {
        return positions.contains(key(level, pos));
    }

    public void add(ServerLevel level, BlockPos pos) {
        if (positions.add(key(level, pos))) setDirty();
    }

    public void remove(ServerLevel level, BlockPos pos) {
        if (positions.remove(key(level, pos))) setDirty();
    }

    public void move(ServerLevel level, BlockPos from, BlockPos to) {
        String fromKey = key(level, from);
        if (!positions.remove(fromKey)) return;
        positions.add(key(level, to));
        setDirty();
    }

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.asLong();
    }
}
