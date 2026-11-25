package dev.hypixelcustomrecipe.entity.voidridden;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class VoidTrailManager {
    private static final Map<ServerLevel, List<Trail>> TRAILS = new IdentityHashMap<>();

    private VoidTrailManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_WORLD_TICK.register(VoidTrailManager::tick);
    }

    public static void add(ServerLevel level, Vec3 start, Vec3 end) {
        if (start.distanceToSqr(end) < 1.0) {
            return;
        }
        TRAILS.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(new Trail(start, end, level.getGameTime() + 100L));
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
            if (now >= trail.expiresAt) {
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
        int count = Math.max(1, (int) Math.ceil(line.length() * 2.0));
        for (int i = 0; i <= count; i++) {
            Vec3 point = trail.start.add(line.scale(i / (double) count));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z,
                    1, 0.08, 0.08, 0.08, 0.0);
        }
    }

    private static void damageNearbyPlayers(ServerLevel level, Trail trail) {
        Vec3 line = trail.end.subtract(trail.start);
        double lengthSquared = line.lengthSqr();
        if (lengthSquared < 1.0E-6) {
            return;
        }

        AABB bounds = new AABB(trail.start, trail.end).inflate(1.0, 1.0, 1.0);
        for (Player player : level.getEntitiesOfClass(Player.class, bounds)) {
            if (!player.isAlive()) {
                continue;
            }

            Vec3 fromStart = player.position().subtract(trail.start);
            double along = Math.max(0.0, Math.min(1.0, fromStart.dot(line) / lengthSquared));
            Vec3 closestPoint = trail.start.add(line.scale(along));
            if (player.position().distanceToSqr(closestPoint) <= 1.0) {
                player.hurtServer(level, level.damageSources().outOfWorld(), 50.0F);
            }
        }
    }

    private record Trail(Vec3 start, Vec3 end, long expiresAt) {
    }
}
