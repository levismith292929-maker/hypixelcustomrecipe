package dev.hypixelcustomrecipe.entity.voidridden;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VoidTrailManager {
    private static final double TRAIL_HALF_WIDTH = 1.25;
    private static final double TRAIL_HEIGHT = 4.0;
    private static final double SAMPLE_SPACING = 0.20;
    private static final Map<ServerLevel, List<Trail>> TRAILS = new IdentityHashMap<>();

    private VoidTrailManager() {}

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(VoidTrailManager::tick);
    }

    public static void add(ServerLevel level, UUID ownerId, Vec3 start, Vec3 end) {
        if (start.distanceToSqr(end) < 1.0) return;
        Vec3 line = end.subtract(start);
        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 horizontal = new Vec3(line.x, 0.0D, line.z);
        float yaw = (float) Math.atan2(horizontal.x, horizontal.z);
        Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
        if (display != null) {
            display.setBlockState(Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotationY(yaw),
                    new Vector3f((float) (TRAIL_HALF_WIDTH * 2.0D), (float) TRAIL_HEIGHT,
                            (float) Math.max(1.0D, horizontal.length())), new Quaternionf()));
            display.setNoGravity(true);
            display.setPos(midpoint.x, midpoint.y + TRAIL_HEIGHT * 0.5D, midpoint.z);
            if (!level.addFreshEntity(display)) display = null;
        }
        TRAILS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new Trail(ownerId, start, end, display));
    }

    private static void tick(ServerLevel level) {
        List<Trail> trails = TRAILS.get(level);
        if (trails == null || trails.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Trail> iterator = trails.iterator();
        while (iterator.hasNext()) {
            Trail trail = iterator.next();
            var owner = level.getEntity(trail.ownerId);
            if (!(owner instanceof VoidriddenEnderman enderman)
                    || !enderman.isAlive()) {
                if (trail.display != null && !trail.display.isRemoved()) trail.display.discard();
                iterator.remove();
                continue;
            }
            if (now % 10L == 0L) damagePlayersAlongEntireTrail(level, trail);
        }
        if (trails.isEmpty()) TRAILS.remove(level);
    }

    private static void damagePlayersAlongEntireTrail(ServerLevel level, Trail trail) {
        Vec3 line = trail.end.subtract(trail.start);
        double length = line.length();
        if (length < 1.0E-6) return;
        AABB searchBounds = new AABB(trail.start, trail.end)
                .inflate(TRAIL_HALF_WIDTH + 1.0, TRAIL_HEIGHT + 1.0, TRAIL_HALF_WIDTH + 1.0);
        for (Player player : level.getEntitiesOfClass(Player.class, searchBounds)) {
            if (!player.isAlive()) continue;
            AABB playerBounds = player.getBoundingBox();
            int samples = Math.max(1, (int) Math.ceil(length / SAMPLE_SPACING));
            boolean insideTrail = false;
            for (int i = 0; i <= samples; i++) {
                Vec3 point = trail.start.add(line.scale(i / (double) samples));
                AABB section = new AABB(
                        point.x - TRAIL_HALF_WIDTH, point.y - 0.25, point.z - TRAIL_HALF_WIDTH,
                        point.x + TRAIL_HALF_WIDTH, point.y + TRAIL_HEIGHT, point.z + TRAIL_HALF_WIDTH
                );
                if (playerBounds.intersects(section)) {
                    insideTrail = true;
                    break;
                }
            }
            if (insideTrail) player.hurtServer(level, level.damageSources().generic(), 10.0F);
        }
    }

    private record Trail(UUID ownerId, Vec3 start, Vec3 end, Display.BlockDisplay display) {}
}
