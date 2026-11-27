package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Storm phase of the Wither Altar encounter. */
public final class StormEncounter {
    private static final double BOSS_HEALTH = 18_000_000.0D;
    private static final double ENTITY_HEALTH = 1_024.0D;
    private static final double BOSS_HP_PER_ENTITY_HP = BOSS_HEALTH / ENTITY_HEALTH;
    private static final double REWARD_THRESHOLD = BOSS_HEALTH * 0.20D;
    private static final int BREAK_TICKS = 5 * 60 * 20;
    private static final int ARENA_HALF_SIZE = 50;
    private static final int SAFE_ZONE_COUNT = 5;
    // Each safe zone is 4 blocks across (2-block radius).
    private static final double SAFE_ZONE_RADIUS = 2.0D;
    private static final int WARNING_TICKS = 160; // 8 seconds to reach a safe zone before the simultaneous strike
    private static final int WAVE_INTERVAL_TICKS = 100;

    private static ServerLevel level;
    private static BlockPos arenaCenter;
    private static UUID altarOwner;
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final Map<UUID, Double> PLAYER_DAMAGE = new HashMap<>();
    private static final List<SafeZone> SAFE_ZONES = new ArrayList<>();
    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final ServerBossEvent BOSS_BAR = new ServerBossEvent(
            UUID.randomUUID(), Component.literal("Storm"), BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS);
    private static WitherBoss storm;
    private static int breakTimer;
    private static int warningTimer = -1;
    private static int waveTimer = WAVE_INTERVAL_TICKS;
    private static boolean testMode;

    private StormEncounter() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                storm == null || entity != storm
                        || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (storm != null && entity == storm) {
                finishSuccessfully();
            } else if (entity instanceof ServerPlayer player
                    && PARTICIPANTS.contains(player.getUUID())) {
                abortBecausePlayerDied(player);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(StormEncounter::tick);
    }

    /** Called immediately after Maxor is defeated. */
    public static void beginBreak(ServerLevel encounterLevel, BlockPos center,
                                  UUID owner, Set<UUID> participants) {
        testMode = false;
        level = encounterLevel;
        arenaCenter = center.immutable();
        altarOwner = owner;
        PARTICIPANTS.clear();
        PARTICIPANTS.addAll(participants);
        PLAYER_DAMAGE.clear();
        SAFE_ZONES.clear();
        STRIKES.clear();
        storm = null;
        breakTimer = BREAK_TICKS;
        warningTimer = -1;
        waveTimer = WAVE_INTERVAL_TICKS;
        BOSS_BAR.setName(Component.literal("Storm begins in 5:00"));
        BOSS_BAR.setProgress(1.0F);
        BOSS_BAR.removeAllPlayers();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (PARTICIPANTS.contains(player.getUUID())) BOSS_BAR.addPlayer(player);
        }
    }

    private static void tick(MinecraftServer server) {
        if (level == null) return;
        if (storm == null) {
            if (--breakTimer <= 0) summonStorm();
            else {
                int seconds = (breakTimer + 19) / 20;
                int minutes = seconds / 60;
                int remainder = seconds % 60;
                BOSS_BAR.setName(Component.literal(String.format(java.util.Locale.ROOT,
                        "Storm begins in %d:%02d", minutes, remainder)));
                BOSS_BAR.setProgress(Math.max(0.0F, Math.min(1.0F,
                        breakTimer / (float) BREAK_TICKS)));
                updateBossBarPlayers(server);
            }
            return;
        }
        if (!storm.isAlive() || storm.isRemoved()) return;

        ServerPlayer target = nearestPlayer();
        if (target != null) storm.setTarget(target);

        float progress = Math.max(0.0F, Math.min(1.0F,
                storm.getHealth() / (float) ENTITY_HEALTH));
        BOSS_BAR.setProgress(progress);
        long hp = Math.max(0L, Math.round(progress * BOSS_HEALTH));
        BOSS_BAR.setName(Component.literal("Storm: "
                + String.format(java.util.Locale.ROOT, "%,d", hp)
                + " / 18,000,000 HP"));
        updateBossBarPlayers(server);

        if (warningTimer >= 0) {
            renderSafeZones();
            warningTimer--;
            if (warningTimer < 0) {
                // Spawn every strike in the same server tick so the whole arena is hit at once.
                for (Strike strike : STRIKES) {
                    strike(strike);
                }
                damagePlayersCaughtInBarrage();
                SAFE_ZONES.clear();
                STRIKES.clear();
                waveTimer = WAVE_INTERVAL_TICKS;
            }
        } else if (--waveTimer <= 0) {
            beginLightningWave();
        }
    }

    public static boolean summonForTesting(ServerPlayer player) {
        if (level != null) return false;
        level = (ServerLevel) player.level();
        arenaCenter = player.blockPosition().immutable();
        altarOwner = player.getUUID();
        PARTICIPANTS.clear();
        PARTICIPANTS.add(player.getUUID());
        PLAYER_DAMAGE.clear();
        SAFE_ZONES.clear();
        STRIKES.clear();
        breakTimer = 0;
        warningTimer = -1;
        waveTimer = WAVE_INTERVAL_TICKS;
        testMode = true;
        BOSS_BAR.removeAllPlayers();
        BOSS_BAR.addPlayer(player);
        return summonStorm();
    }

    private static boolean summonStorm() {
        WitherBoss boss = EntityTypes.WITHER.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) {
            abortAndReturnAltar("Storm could not be summoned.");
            return false;
        }
        boss.setPos(arenaCenter.getX() + 0.5D, arenaCenter.getY(),
                arenaCenter.getZ() + 0.5D);
        boss.setCustomName(Component.literal("Storm").withStyle(style ->
                style.withColor(0x5555FF).withBold(true)));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        var maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(ENTITY_HEALTH);
        boss.setHealth((float) ENTITY_HEALTH);
        ServerPlayer target = nearestPlayer();
        if (target != null) boss.setTarget(target);
        if (!level.addFreshEntity(boss)) {
            abortAndReturnAltar("Storm could not be spawned.");
            return false;
        }
        storm = boss;
        breakTimer = 0;
        waveTimer = WAVE_INTERVAL_TICKS;
        BOSS_BAR.setName(Component.literal("Storm: 18,000,000 / 18,000,000 HP"));
        BOSS_BAR.setProgress(1.0F);
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.0F, 0.9F);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(testMode ? "STORM TEST SUMMONED!" : "STORM HAS AWAKENED!").withStyle(style ->
                        style.withColor(0x5555FF).withBold(true)), false);
        return true;
    }

    private static void beginLightningWave() {
        SAFE_ZONES.clear();
        STRIKES.clear();
        for (int i = 0; i < SAFE_ZONE_COUNT; i++) {
            for (int attempt = 0; attempt < 200; attempt++) {
                double x = arenaCenter.getX() + (level.getRandom().nextDouble() * 2.0D - 1.0D)
                        * (ARENA_HALF_SIZE - SAFE_ZONE_RADIUS);
                double z = arenaCenter.getZ() + (level.getRandom().nextDouble() * 2.0D - 1.0D)
                        * (ARENA_HALF_SIZE - SAFE_ZONE_RADIUS);
                boolean overlaps = false;
                for (SafeZone existing : SAFE_ZONES) {
                    double dx = existing.x - x;
                    double dz = existing.z - z;
                    if (dx * dx + dz * dz < 4.0D * SAFE_ZONE_RADIUS * SAFE_ZONE_RADIUS) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    SAFE_ZONES.add(new SafeZone(x, z));
                    break;
                }
            }
        }
        // Place one lightning strike at the center of every block in the 100 x 100 arena,
        // excluding every block whose center falls inside one of the safe zones.
        for (int xOffset = -ARENA_HALF_SIZE; xOffset < ARENA_HALF_SIZE; xOffset++) {
            for (int zOffset = -ARENA_HALF_SIZE; zOffset < ARENA_HALF_SIZE; zOffset++) {
                double x = arenaCenter.getX() + xOffset + 0.5D;
                double z = arenaCenter.getZ() + zOffset + 0.5D;
                if (!isSafe(x, z)) {
                    STRIKES.add(new Strike(x, z));
                }
            }
        }
        warningTimer = WARNING_TICKS;
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Storm is charging a lightning barrage! Find the green safe zones!")
                        .withStyle(style -> style.withColor(0x55FF55)), false);
    }

    private static void renderSafeZones() {
        double y = arenaCenter.getY() + 0.15D;
        for (SafeZone zone : SAFE_ZONES) {
            for (int i = 0; i < 48; i++) {
                double angle = Math.PI * 2.0D * i / 48.0D;
                double x = zone.x + Math.cos(angle) * SAFE_ZONE_RADIUS;
                double z = zone.z + Math.sin(angle) * SAFE_ZONE_RADIUS;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, zone.x, y, zone.z,
                    12, SAFE_ZONE_RADIUS * 0.55D, 0.02D, SAFE_ZONE_RADIUS * 0.55D, 0.0D);
        }
    }

    private static boolean isSafe(double x, double z) {
        for (SafeZone zone : SAFE_ZONES) {
            double dx = x - zone.x;
            double dz = z - zone.z;
            if (dx * dx + dz * dz <= SAFE_ZONE_RADIUS * SAFE_ZONE_RADIUS) return true;
        }
        return false;
    }

    private static void strike(Strike strike) {
        int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(strike.x), (int) Math.floor(strike.z));
        double y = Math.max(arenaCenter.getY(), groundY);
        // Use the vanilla lightning entity for its normal bolt visuals, but keep its
        // vanilla damage/fire disabled because this encounter applies its own true damage.
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.setPos(strike.x, y, strike.z);
            level.addFreshEntity(bolt);
        }

    }

    private static void damagePlayersCaughtInBarrage() {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive()) continue;
            double xOffset = player.getX() - arenaCenter.getX();
            double zOffset = player.getZ() - arenaCenter.getZ();
            if (xOffset < -ARENA_HALF_SIZE || xOffset >= ARENA_HALF_SIZE
                    || zOffset < -ARENA_HALF_SIZE || zOffset >= ARENA_HALF_SIZE
                    || isSafe(player.getX(), player.getZ())) continue;
            int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    player.blockPosition().getX(), player.blockPosition().getZ());
            if (Math.abs(player.getY() - Math.max(arenaCenter.getY(), groundY)) > 5.0D) continue;

            // The entire arena is struck at once, but each player can only take one hit per wave.
            PARTICIPANTS.add(player.getUUID());
            SkyblockPlayerState state = SkyblockPlayerState.get(player);
            float damage = (float) (state.maximumHealth() * 0.10D);
            boolean defeated = state.applyTrueDamage(damage);
            DamageNumberHandler.show(level, player, damage, false);
            player.sendSystemMessage(Component.literal("Storm Evaporated you")
                    .withStyle(style -> style.withColor(0x5555FF)));
            if (defeated) {
                player.setHealth(0.0F);
                player.die(level.damageSources().lightningBolt());
            }
        }
    }

    private static ServerPlayer nearestPlayer() {
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive()
                    || player.distanceToSqr(Vec3.atCenterOf(arenaCenter)) > 100.0D * 100.0D) continue;
            double distance = player.distanceToSqr(Vec3.atCenterOf(arenaCenter));
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void updateBossBarPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level
                    && player.distanceToSqr(Vec3.atCenterOf(arenaCenter)) <= 100.0D * 100.0D) {
                BOSS_BAR.addPlayer(player);
                PARTICIPANTS.add(player.getUUID());
            } else {
                BOSS_BAR.removePlayer(player);
            }
        }
    }

    /** Called by Soul Blade's direct true-damage path. */
    public static void recordDirectDamage(LivingEntity target, ServerPlayer attacker,
                                          double entityDamage) {
        if (storm == null || target != storm || entityDamage <= 0.0D) return;
        PLAYER_DAMAGE.merge(attacker.getUUID(), entityDamage * BOSS_HP_PER_ENTITY_HP, Double::sum);
        PARTICIPANTS.add(attacker.getUUID());
    }

    private static void finishSuccessfully() {
        MinecraftServer server = level.getServer();
        int rewarded = 0;
        if (!testMode) {
            for (Map.Entry<UUID, Double> entry : PLAYER_DAMAGE.entrySet()) {
                if (entry.getValue() < REWARD_THRESHOLD) continue;
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null) continue;
                SkyblockPlayerState.get(player).addLevelExperience(player, 5_000);
                player.sendSystemMessage(Component.literal("Storm defeated! +100 Player Levels")
                        .withStyle(style -> style.withColor(0x55FF55).withBold(true)));
                rewarded++;
            }
        }
        BOSS_BAR.removeAllPlayers();
        level.playSound(null, storm.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE, 2.0F, 0.9F);
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                (testMode ? "Storm test encounter completed. No progression or rewards were triggered." : "Storm has been defeated! " + rewarded + " player(s) earned the reward.")), false);
        clear();
    }

    private static void abortBecausePlayerDied(ServerPlayer deadPlayer) {
        if (level == null) return;
        MinecraftServer server = level.getServer();
        if (storm != null) storm.discard();
        abortAndReturnAltar(deadPlayer.getName().getString() + " died. The Storm encounter has ended.");
    }

    private static void abortAndReturnAltar(String message) {
        if (level == null) return;
        MinecraftServer server = level.getServer();
        if (storm != null) storm.discard();
        ServerPlayer owner = testMode ? null : server.getPlayerList().getPlayer(altarOwner);
        if (owner != null) {
            net.minecraft.world.item.ItemStack altar = new net.minecraft.world.item.ItemStack(ModItems.WITHER_ALTAR);
            if (!owner.getInventory().add(altar)) owner.drop(altar, false);
            owner.sendSystemMessage(Component.literal("The encounter failed. Your Wither Altar has been returned.")
                    .withStyle(style -> style.withColor(0xFF5555)));
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        BOSS_BAR.removeAllPlayers();
        clear();
    }

    private static void clear() {
        storm = null;
        testMode = false;
        level = null;
        arenaCenter = null;
        altarOwner = null;
        breakTimer = 0;
        warningTimer = -1;
        waveTimer = WAVE_INTERVAL_TICKS;
        PARTICIPANTS.clear();
        PLAYER_DAMAGE.clear();
        SAFE_ZONES.clear();
        STRIKES.clear();
    }

    private record SafeZone(double x, double z) {}
    private record Strike(double x, double z) {}
}
