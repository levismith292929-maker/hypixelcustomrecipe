package dev.hypixelcustomrecipe.entity.voidridden;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
        TRAILS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new Trail(ownerId, start, end));
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
                iterator.remove();
                continue;
            }
            if (now % 4L == 0L) spawnTrailParticles(level, trail);
            if (now % 10L == 0L) damagePlayersAlongEntireTrail(level, trail);
        }
        if (trails.isEmpty()) TRAILS.remove(level);
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
            for (int widthStep = -1; widthStep <= 1; widthStep++) {
                for (int heightStep = 0; heightStep <= 4; heightStep++) {
                    Vec3 particle = point.add(side.scale(widthStep)).add(0.0, heightStep, 0.0);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false,
                            particle.x, particle.y, particle.z, 1, 0.08, 0.08, 0.08, 0.0);
                }
            }
        }
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

    private record Trail(UUID ownerId, Vec3 start, Vec3 end) {}
}
