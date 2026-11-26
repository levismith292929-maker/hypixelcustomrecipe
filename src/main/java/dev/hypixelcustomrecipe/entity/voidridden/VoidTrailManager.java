package dev.hypixelcustomrecipe.entity.voidridden;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoidTrailManager {
    // The trail forms a 2-block-wide by 4-block-tall damaging corridor.
    private static final double TRAIL_HALF_WIDTH = 1.0;
    private static final double TRAIL_HEIGHT = 4.0;
    private static final Map<ServerLevel, List<Trail>> TRAILS = new IdentityHashMap<>();

    private VoidTrailManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(VoidTrailManager::tick);
    }

    public static void add(ServerLevel level, UUID ownerId, Vec3 start, Vec3 end) {
        if (start.distanceToSqr(end) < 1.0) {
            return;
        }
        TRAILS.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(new Trail(ownerId, start, end));
    }

    private static void tick(ServerLevel level) {
        List<Trail> trails = TRAILS.get(level);
        if (trails == null || trails.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        Iterator<Trail> iterator = trails.iterator();
        while (iterator.hasNext()) {
            Trail trail = iterator.next();
            Entity owner = level.getEntity(trail.ownerId);
            if (owner instanceof net.minecraft.world.entity.LivingEntity living && !living.isAlive()) {
                iterator.remove();
                continue;
            }
            if (now % 4L == 0L) {
                spawnTrailParticles(level, trail);
            }
            if (now % 20L == 0L) {
                damageNearbyPlayers(level, trail);
            }
        }

        if (trails.isEmpty()) {
            TRAILS.remove(level);
        }
    }

    private static void spawnTrailParticles(ServerLevel level, Trail trail) {
        Vec3 line = trail.end.subtract(trail.start);
        Vec3 horizontal = new Vec3(line.x, 0.0, line.z);
        Vec3 side = horizontal.lengthSqr() < 1.0E-6
                ? new Vec3(1.0, 0.0, 0.0)
                : new Vec3(-horizontal.z, 0.0, horizontal.x).normalize();

        int count = Math.max(1, (int) Math.ceil(line.length() * 2.0));
        for (int i = 0; i <= count; i++) {
            Vec3 point = trail.start.add(line.scale(i / (double) count));
            // Fill the complete cross-section: 2 blocks across and 4 blocks high.
            for (int widthStep = -1; widthStep <= 1; widthStep++) {
                for (int heightStep = 0; heightStep <= 4; heightStep++) {
                    Vec3 particle = point.add(side.scale(widthStep)).add(0.0, heightStep, 0.0);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false,
                            particle.x, particle.y, particle.z, 1, 0.08, 0.08, 0.08, 0.0);
                }
            }
        }
    }

    private static void damageNearbyPlayers(ServerLevel level, Trail trail) {
        Vec3 line = trail.end.subtract(trail.start);
        Vec3 horizontal = new Vec3(line.x, 0.0, line.z);
        double lengthSquared = horizontal.lengthSqr();
        if (lengthSquared < 1.0E-6) {
            return;
        }

        Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize();
        AABB bounds = new AABB(trail.start, trail.end)
                .inflate(TRAIL_HALF_WIDTH, TRAIL_HEIGHT, TRAIL_HALF_WIDTH)
                .expandTowards(0.0, TRAIL_HEIGHT, 0.0);
        for (Player player : level.getEntitiesOfClass(Player.class, bounds)) {
            if (!player.isAlive()) {
                continue;
            }

            Vec3 fromStart = player.position().subtract(trail.start);
            double along = Math.max(0.0, Math.min(1.0,
                    (fromStart.x * horizontal.x + fromStart.z * horizontal.z) / lengthSquared));
            Vec3 closestPoint = trail.start.add(line.scale(along));
            Vec3 horizontalOffset = player.position().subtract(closestPoint);
            double sideDistance = Math.abs(horizontalOffset.dot(side));
            boolean overlapsHeight = player.getBoundingBox().maxY >= closestPoint.y
                    && player.getBoundingBox().minY <= closestPoint.y + TRAIL_HEIGHT;
            if (sideDistance <= TRAIL_HALF_WIDTH && overlapsHeight) {
                player.hurtServer(level, level.damageSources().generic(), 50.0F);
            }
        }
    }

    private record Trail(UUID ownerId, Vec3 start, Vec3 end) {
    }
}
