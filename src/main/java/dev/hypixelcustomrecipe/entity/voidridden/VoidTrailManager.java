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
    private static final double TRAIL_HALF_WIDTH = 1.25D;
    private static final double TRAIL_HEIGHT = 4.0D;
    private static final double DAMAGE_SAMPLE_SPACING = 0.5D;
    private static final double PARTICLE_SAMPLE_SPACING = 0.45D;
    private static final Map<ServerLevel, List<Trail>> TRAILS = new IdentityHashMap<>();

    private VoidTrailManager() {}

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(VoidTrailManager::tick);
    }

    public static void add(ServerLevel level, UUID ownerId, Vec3 start, Vec3 end) {
        if (start.distanceToSqr(end) < 1.0D) return;
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
            if (!(owner instanceof VoidriddenEnderman enderman) || !enderman.isAlive()) {
                iterator.remove();
                continue;
            }
            if (now % 2L == 0L) emitTrailParticles(level, trail);
            if (now % 10L == 0L) damagePlayersAlongEntireTrail(level, trail);
        }
        if (trails.isEmpty()) TRAILS.remove(level);
    }

    private static void emitTrailParticles(ServerLevel level, Trail trail) {
        Vec3 line = trail.end.subtract(trail.start);
        double length = line.length();
        if (length < 1.0E-6D) return;
        int samples = Math.max(1, (int) Math.ceil(length / PARTICLE_SAMPLE_SPACING));
        for (int i = 0; i <= samples; i++) {
            Vec3 point = trail.start.add(line.scale(i / (double) samples));
            // Bright end-rod sparks layered with purple portal motes make the trail easier to see.
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y + 0.5D, point.z,
                    2, 0.10D, 0.18D, 0.10D, 0.015D);
            level.sendParticles(ParticleTypes.PORTAL, point.x, point.y + 0.5D, point.z,
                    2, 0.16D, 0.22D, 0.16D, 0.015D);
        }
    }

    private static void damagePlayersAlongEntireTrail(ServerLevel level, Trail trail) {
        Vec3 line = trail.end.subtract(trail.start);
        double length = line.length();
        if (length < 1.0E-6D) return;
        AABB searchBounds = new AABB(trail.start, trail.end)
                .inflate(TRAIL_HALF_WIDTH + 1.0D, TRAIL_HEIGHT + 1.0D, TRAIL_HALF_WIDTH + 1.0D);
        int samples = Math.max(1, (int) Math.ceil(length / DAMAGE_SAMPLE_SPACING));
        for (Player player : level.getEntitiesOfClass(Player.class, searchBounds)) {
            if (!player.isAlive()) continue;
            AABB playerBounds = player.getBoundingBox();
            boolean insideTrail = false;
            for (int i = 0; i <= samples; i++) {
                Vec3 point = trail.start.add(line.scale(i / (double) samples));
                AABB section = new AABB(
                        point.x - TRAIL_HALF_WIDTH, point.y - 0.25D, point.z - TRAIL_HALF_WIDTH,
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
