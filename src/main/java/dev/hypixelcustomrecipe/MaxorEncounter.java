package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import com.mojang.math.Transformation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * First floor of the Wither Altar encounter: Maxor.
 * Damage contribution is recorded before vanilla mitigation is applied.
 */
public final class MaxorEncounter {
    private static final double MAXOR_HEALTH = 25_000_000.0D;
    // Vanilla's max-health attribute is capped, so map 1,024 entity HP to 25,000,000 boss HP.
    private static final double ENTITY_HEALTH = 1_024.0D;
    private static final double BOSS_HP_PER_ENTITY_HP = MAXOR_HEALTH / ENTITY_HEALTH;
    private static final double REWARD_THRESHOLD = MAXOR_HEALTH * 0.20D;
    private static final int MINION_INTERVAL_TICKS = 80;
    private static final int MINION_BATCH_SIZE = 8;
    private static final int MAX_LIVING_MINIONS = 48;

    private static WitherBoss maxor;
    private static final Set<UUID> LASER_AFFECTED = new HashSet<>();
    private static final Map<UUID, Integer> LASER_HIT_COOLDOWNS = new HashMap<>();
    private static ServerLevel encounterLevel;
    private static UUID altarOwner;
    private static int minionSpawnTimer;
    private static int laserBeamTimer;
    private static boolean maxorVulnerable = true;
    private static int stationaryLaserTimer = 300;
    private static BlockDisplay stationaryLaser;
    private static boolean stationaryLaserActive;
    private static int crystalChallengeTimer = 400;
    private static int crystalChallengeTicks;
    private static final List<UUID> challengeCrystals = new ArrayList<>();
    private static final DustParticleOptions LASER_PARTICLE = new DustParticleOptions(0xAA00FF, 0.85F);
    private static final double LASER_HIT_RADIUS = 0.22D;
    private static final int LASER_CHARGE_TICKS = 12;
    private static int laserChargeTicks;
    private static int laserBeamTicks;
    private static Vec3 laserShotStart;
    private static Vec3 laserShotEnd;
    // Vanilla Withers regenerate naturally; remember the last legitimate HP value
    // so Maxor cannot recover health during the encounter.
    private static float lastKnownHealth;
    private static boolean testMode;
    private static final Map<UUID, Double> PLAYER_DAMAGE = new HashMap<>();
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final Set<UUID> DEAD_PARTICIPANTS = new HashSet<>();
    private static final List<UUID> MINIONS = new ArrayList<>();
    private static final ServerBossEvent BOSS_BAR = new ServerBossEvent(
            UUID.randomUUID(),
            Component.literal("Maxor"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private MaxorEncounter() {
    }

    public static boolean isActive() { return maxor != null && maxor.isAlive(); }
    public static boolean isMaxor(LivingEntity entity) { return maxor != null && entity == maxor; }
    /** Soul Blade and other direct-damage paths must respect Maxor's laser immunity. */
    public static boolean canDamageMaxor() { return maxor == null || maxorVulnerable; }
    public static boolean isMaxorMinion(UUID entityId) { return MINIONS.contains(entityId); }

    /** Maxor's displayed/logical health; vanilla Wither health is only the backing pool. */
    public static double logicalMaximumHealth() { return MAXOR_HEALTH; }

    /** Converts logical boss damage into the Wither's vanilla-compatible backing health. */
    public static double toEntityDamage(double logicalDamage) {
        return logicalDamage * ENTITY_HEALTH / MAXOR_HEALTH;
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack held = player.getItemInHand(hand);
            if (!held.is(ModItems.WITHER_ALTAR)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.FAIL;
            }
            if (maxor != null && maxor.isAlive()) {
                serverPlayer.sendSystemMessage(Component.literal("A boss encounter is already active.")
                        .withStyle(style -> style.withColor(0xFF5555)));
                return InteractionResult.FAIL;
            }
            start(serverPlayer, serverLevel, false);
            if (!serverPlayer.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.SUCCESS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(entity instanceof EndCrystal) || level.isClientSide()
                    || !challengeCrystals.contains(entity.getUUID())
                    || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            // Explicitly route attacks to the encounter crystals. This keeps the
            // challenge breakable even if another combat hook interferes with vanilla hits.
            entity.setInvulnerable(false);
            entity.hurtServer(serverLevel, player.damageSources().playerAttack(player), 1.0F);
            return InteractionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // Keep Maxor immune to normal attacks, but let operator /kill work.
            // Soul Blade uses its separate direct true-damage path.
            if (maxor != null && entity == maxor && !maxorVulnerable
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return false;
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && PARTICIPANTS.contains(player.getUUID())) {
                DEAD_PARTICIPANTS.add(player.getUUID());
                if (maxor != null && allFightersDead(player.level().getServer())) {
                    abortBecauseAllFightersDied();
                    return;
                }
            }
            if (maxor != null && entity.getUUID().equals(maxor.getUUID())) {
                if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    // /kill must reset the phase, not count as a legitimate boss clear.
                    abortEncounter("Maxor was removed. The encounter has been reset.");
                } else {
                    finishSuccessfully();
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(MaxorEncounter::tick);
    }

    public static boolean summonForTesting(ServerPlayer player) {
        if (maxor != null && maxor.isAlive()) return false;
        return start(player, (ServerLevel) player.level(), true);
    }

    private static boolean start(ServerPlayer starter, ServerLevel level, boolean testing) {
        WitherBoss boss = EntityTypes.WITHER.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) {
            starter.sendSystemMessage(Component.literal("Maxor could not be summoned here.")
                    .withStyle(style -> style.withColor(0xFF5555)));
            return false;
        }

        BlockPos spawnPos = starter.blockPosition().relative(starter.getDirection(), 5);
        boss.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        boss.setCustomName(Component.literal("Maxor").withStyle(style -> style
                .withColor(0xAA00AA).withBold(true)));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        var maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(ENTITY_HEALTH);
        }
        boss.setHealth((float) ENTITY_HEALTH);
        boss.setTarget(starter);

        if (!level.addFreshEntity(boss)) {
            starter.sendSystemMessage(Component.literal("Maxor could not be spawned here.")
                    .withStyle(style -> style.withColor(0xFF5555)));
            return false;
        }

        BossArena.ensure(level, spawnPos.below());
        maxor = boss;
        testMode = testing;
        encounterLevel = level;
        altarOwner = starter.getUUID();
        minionSpawnTimer = MINION_INTERVAL_TICKS;
        laserBeamTimer = 0;
        maxorVulnerable = true;
        stationaryLaserTimer = 300;
        stationaryLaserActive = false;
        crystalChallengeTimer = 400;
        crystalChallengeTicks = 0;
        challengeCrystals.clear();
        laserChargeTicks = 0;
        laserBeamTicks = 0;
        laserShotStart = null;
        laserShotEnd = null;
        lastKnownHealth = (float) ENTITY_HEALTH;
        PLAYER_DAMAGE.clear();
        PARTICIPANTS.clear();
        DEAD_PARTICIPANTS.clear();
        LASER_AFFECTED.clear();
        LASER_HIT_COOLDOWNS.clear();
        MINIONS.clear();
        BOSS_BAR.setName(Component.literal("Maxor: 25,000,000 / 25,000,000 HP"));
        BOSS_BAR.setProgress(1.0F);
        BOSS_BAR.addPlayer(starter);
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.0F, 0.7F);
        starter.sendSystemMessage(Component.literal(testing ? "MAXOR TEST SUMMONED!" : "MAXOR HAS AWAKENED!")
                .withStyle(style -> style.withColor(0xAA00AA).withBold(true)));
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (maxor == null || encounterLevel == null) {
            return;
        }
        if (!maxor.isAlive() || maxor.isRemoved()) {
            return;
        }
        if (allFightersDead(server)) {
            abortBecauseAllFightersDied();
            return;
        }

        // Vanilla Withers regenerate over time. Maxor must not heal, so discard
        // any increase above the last health value observed by the encounter.
        float currentHealth = maxor.getHealth();
        if (currentHealth > lastKnownHealth) {
            maxor.setHealth(lastKnownHealth);
        } else {
            lastKnownHealth = currentHealth;
        }

        // Keep Maxor focused on actual players and prevent the vanilla Wither
        // flight AI from carrying him far above the arena.
        ServerPlayer target = nearestPlayerInEncounter();
        if (target != null) {
            maxor.setTarget(target);
            Vec3 delta = target.position().subtract(maxor.position());
            Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
            if (horizontal.lengthSqr() > 2.25D) {
                Vec3 motion = horizontal.normalize().scale(0.20D);
                maxor.setDeltaMovement(motion.x,
                        Math.max(-0.08D, Math.min(0.08D, delta.y * 0.035D)), motion.z);
            }
            double maximumY = target.getY() + 2.0D;
            if (maxor.getY() > maximumY) {
                maxor.setPos(maxor.getX(), maximumY, maxor.getZ());
                Vec3 motion = maxor.getDeltaMovement();
                maxor.setDeltaMovement(motion.x, Math.min(motion.y, -0.12D), motion.z);
            }
        }
        tickCrystalChallenge(server);
        tickStationaryLaser();
        LASER_HIT_COOLDOWNS.replaceAll((uuid, ticks) -> ticks - 1);
        LASER_HIT_COOLDOWNS.values().removeIf(ticks -> ticks <= 0);

        if (laserChargeTicks > 0) {
            renderLaserParticles(laserShotStart, laserShotEnd, 0.55F);
            if (--laserChargeTicks <= 0) {
                laserBeamTicks = 8;
            }
        } else if (laserBeamTicks > 0) {
            renderLaserParticles(laserShotStart, laserShotEnd, 0.95F);
            applyLaserBeamHits();
            laserBeamTicks--;
            if (laserBeamTicks <= 0) {
                laserShotStart = null;
                laserShotEnd = null;
            }
        }

        // A short visible charge-up gives players time to move out of the narrow beam.
        if (--laserBeamTimer <= 0) {
            if (target != null) {
                beginLaserCharge(target);
            }
            laserBeamTimer = 100;
        }

        float progress = Math.max(0.0F, Math.min(1.0F, maxor.getHealth() / (float) ENTITY_HEALTH));
        BOSS_BAR.setProgress(progress);
        long displayedHp = Math.max(0L, Math.round(progress * MAXOR_HEALTH));
        BOSS_BAR.setName(Component.literal("Maxor: " + String.format(java.util.Locale.ROOT, "%,d", displayedHp)
                + " / 2,000,000 HP"));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == encounterLevel
                    && player.distanceToSqr(maxor) <= 96.0D * 96.0D) {
                BOSS_BAR.addPlayer(player);
            } else {
                BOSS_BAR.removePlayer(player);
            }
        }

        if (--minionSpawnTimer <= 0) {
            minionSpawnTimer = MINION_INTERVAL_TICKS;
            spawnMinionWave();
        }
    }

    /** Locks the beam's aim point during a brief charge-up so players can dodge it. */
    private static void beginLaserCharge(ServerPlayer target) {
        laserShotStart = maxor.getEyePosition();
        laserShotEnd = target.getEyePosition();
        if (laserShotEnd.subtract(laserShotStart).lengthSqr() <= 0.01D) return;
        laserChargeTicks = LASER_CHARGE_TICKS;
        encounterLevel.playSound(null, maxor.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, 1.2F, 1.6F);
    }

    /** Draws the beam as particles only; particles have no entity-sized collision box. */
    private static void renderLaserParticles(Vec3 start, Vec3 end, float scale) {
        if (start == null || end == null) return;
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance <= 0.0D) return;
        Vec3 step = delta.scale(0.5D / distance);
        int count = Math.max(1, (int) Math.ceil(distance / 0.5D));
        DustParticleOptions particle = new DustParticleOptions(0xAA00FF, scale);
        for (int i = 0; i <= count; i++) {
            Vec3 point = start.add(step.scale(i));
            encounterLevel.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Only players close to the actual beam segment are hit; being the target is not enough. */
    private static void applyLaserBeamHits() {
        if (laserShotStart == null || laserShotEnd == null) return;
        Vec3 segment = laserShotEnd.subtract(laserShotStart);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared <= 0.0001D) return;
        double radiusSquared = LASER_HIT_RADIUS * LASER_HIT_RADIUS;
        for (ServerPlayer player : encounterLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() != encounterLevel || !player.isAlive()
                    || player.isSpectator()
                    || LASER_HIT_COOLDOWNS.containsKey(player.getUUID())) continue;
            // The HP laser also affects creative players. Check the entire hitbox
            // against a slightly forgiving ray so a beam crossing the body registers.
            if (player.getBoundingBox().inflate(0.45D)
                    .clip(laserShotStart, laserShotEnd).isEmpty()) continue;

            SkyblockPlayerState.get(player).addMaxorLaserStack();
            LASER_AFFECTED.add(player.getUUID());
            LASER_HIT_COOLDOWNS.put(player.getUUID(), 100);
            player.sendSystemMessage(Component.literal("Maxor's laser reduced your maximum Health by 2%!")
                    .withStyle(style -> style.withColor(0xAA00AA)));
        }
    }

    private static void tickCrystalChallenge(MinecraftServer server) {
        if (crystalChallengeTicks > 0) {
            challengeCrystals.removeIf(id -> {
                Entity crystal = encounterLevel.getEntity(id);
                return crystal == null || !crystal.isAlive() || crystal.isRemoved();
            });
            if (--crystalChallengeTicks <= 0) {
                if (!challengeCrystals.isEmpty()) {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        if (player.level() == encounterLevel && player.isAlive()
                                && Math.abs(player.getX() - maxor.blockPosition().getX()) <= 50
                                && Math.abs(player.getZ() - maxor.blockPosition().getZ()) <= 50) {
                            player.setHealth(0.0F);
                            player.die(encounterLevel.damageSources().generic());
                        }
                    }
                }
                for (UUID id : challengeCrystals) {
                    Entity crystal = encounterLevel.getEntity(id);
                    if (crystal != null) crystal.discard();
                }
                challengeCrystals.clear();
                crystalChallengeTimer = 600;
            }
            return;
        }
        if (--crystalChallengeTimer > 0) return;
        int count = 5 + encounterLevel.getRandom().nextInt(4);
        BlockPos center = BossArena.getCenter(encounterLevel, maxor.blockPosition());
        for (int i = 0; i < count; i++) {
            EndCrystal crystal = EntityTypes.END_CRYSTAL.create(encounterLevel, EntitySpawnReason.COMMAND);
            if (crystal == null) continue;
            double angle = Math.PI * 2.0D * i / count;
            double radius = 12.0D + encounterLevel.getRandom().nextDouble() * 22.0D;
            crystal.setPos(center.getX() + 0.5D + Math.cos(angle) * radius,
                    center.getY() + 1.0D, center.getZ() + 0.5D + Math.sin(angle) * radius);
            crystal.setInvulnerable(false);
            if (encounterLevel.addFreshEntity(crystal)) challengeCrystals.add(crystal.getUUID());
        }
        crystalChallengeTicks = 400;
        encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("MAXOR: Destroy every End Crystal! 20 seconds!")
                        .withStyle(s -> s.withColor(0xFF5555).withBold(true)), false);
    }

    private static void tickStationaryLaser() {
        if (!stationaryLaserActive) {
            if (--stationaryLaserTimer > 0) return;
            BlockPos center = BossArena.getCenter(encounterLevel, maxor.blockPosition());
            stationaryLaser = EntityTypes.BLOCK_DISPLAY.create(encounterLevel, EntitySpawnReason.COMMAND);
            if (stationaryLaser == null) { stationaryLaserTimer = 300; return; }
            stationaryLaser.setBlockState(BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:red_stained_glass")).defaultBlockState());
            // A 2x2 vertical pillar, 12 blocks tall. Display entities are visual-only,
            // so players can walk through it; the server-side AABB below handles hits.
            stationaryLaser.setTransformation(new Transformation(
                    new Vector3f(), new Quaternionf(), new Vector3f(2.0F, 12.0F, 2.0F), new Quaternionf()));
            stationaryLaser.setNoGravity(true);
            stationaryLaser.setPos(center.getX() - 1.0D, center.getY(), center.getZ() - 1.0D);
            encounterLevel.addFreshEntity(stationaryLaser);
            stationaryLaserActive = true;
            maxorVulnerable = false;
            encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("MAXOR IS IMMUNE! Lure him into the red laser!")
                            .withStyle(s -> s.withColor(0xFF5555).withBold(true)), false);
        }
        BlockPos center = BossArena.getCenter(encounterLevel, maxor.blockPosition());
        // 2x2 vertical laser pillar, 12 blocks tall. It has no physical collision;
        // only this overlap test makes it lethal, so players can walk through it.
        AABB beam = new AABB(center.getX() - 1.0D, center.getY(),
                center.getZ() - 1.0D, center.getX() + 1.0D, center.getY() + 12.0D,
                center.getZ() + 1.0D);
        if (maxor.getBoundingBox().intersects(beam)) {
            stationaryLaser.discard();
            stationaryLaser = null;
            stationaryLaserActive = false;
            maxorVulnerable = true;
            stationaryLaserTimer = 300;
            encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("MAXOR HIT THE LASER! He is vulnerable again!")
                            .withStyle(s -> s.withColor(0x55FF55).withBold(true)), false);
            return;
        }
        for (ServerPlayer player : encounterLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == encounterLevel && player.isAlive()
                    && player.getBoundingBox().intersects(beam)) {
                player.setHealth(0.0F);
                player.die(encounterLevel.damageSources().generic());
            }
        }
    }

    private static void spawnMinionWave() {
        MINIONS.removeIf(uuid -> {
            Entity entity = encounterLevel.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });
        if (MINIONS.size() >= MAX_LIVING_MINIONS) {
            return;
        }

        int count = Math.min(MINION_BATCH_SIZE, MAX_LIVING_MINIONS - MINIONS.size());
        for (int i = 0; i < count; i++) {
            WitherSkeleton skeleton = EntityTypes.WITHER_SKELETON.create(
                    encounterLevel, EntitySpawnReason.MOB_SUMMONED);
            if (skeleton == null) {
                continue;
            }
            double angle = encounterLevel.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 3.0D + encounterLevel.getRandom().nextDouble() * 4.0D;
            skeleton.setPos(
                    maxor.getX() + Math.cos(angle) * distance,
                    maxor.getY(),
                    maxor.getZ() + Math.sin(angle) * distance
            );
            skeleton.setPersistenceRequired();
            ServerPlayer target = nearestPlayerInEncounter();
            if (target != null) {
                skeleton.setTarget(target);
            }
            if (encounterLevel.addFreshEntity(skeleton)) {
                MINIONS.add(skeleton.getUUID());
            }
        }
    }

    private static ServerPlayer nearestPlayerInEncounter() {
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : encounterLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() != encounterLevel || !player.isAlive()
                    || player.distanceToSqr(maxor) > 64.0D * 64.0D) {
                continue;
            }
            double distance = player.distanceToSqr(maxor);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void finishSuccessfully() {
        MinecraftServer server = encounterLevel.getServer();
        restoreLaserHealth(server);
        int rewarded = 0;
        if (!testMode) {
            for (Map.Entry<UUID, Double> entry : PLAYER_DAMAGE.entrySet()) {
                if (entry.getValue() < REWARD_THRESHOLD) continue;
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    SkyblockPlayerState.get(player).addLevelExperience(player, 5_000);
                    ItemStack maxorPet = new ItemStack(DatapackItems.MAXOR_PET);
                    if (!player.getInventory().add(maxorPet)) player.drop(maxorPet, false);
                    player.sendSystemMessage(Component.literal("Maxor defeated! +100 Player Levels + Maxor Pet")
                            .withStyle(style -> style.withColor(0x55FF55).withBold(true)));
                    rewarded++;
                }
            }
        }
        for (UUID uuid : MINIONS) {
            Entity entity = encounterLevel.getEntity(uuid);
            if (entity != null) {
                entity.discard();
            }
        }
        BOSS_BAR.removeAllPlayers();
        encounterLevel.playSound(null, maxor.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE, 2.0F, 0.8F);
        encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Maxor has been defeated! " + rewarded + " player(s) earned the reward. Storm begins in 2 minutes!"),
                false);
        // Preserve the encounter location, owner, and participants for Storm's
        // five-minute intermission before clearing Maxor's phase state.
        if (testMode) {
            encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("Maxor test encounter completed. No progression or rewards were triggered."), false);
            BossArena.restore();
        } else {
            StormEncounter.beginBreak(encounterLevel, BossArena.getCenter(encounterLevel, maxor.blockPosition()),
                    altarOwner, Set.copyOf(PARTICIPANTS));
        }
        clearEncounter();
    }

    private static boolean allFightersDead(MinecraftServer server) {
        if (PARTICIPANTS.isEmpty()) return false;
        for (UUID id : PARTICIPANTS) {
            ServerPlayer fighter = server.getPlayerList().getPlayer(id);
            if (!DEAD_PARTICIPANTS.contains(id) && fighter != null && fighter.isAlive()) return false;
        }
        return true;
    }

    private static void abortBecauseAllFightersDied() {
        abortEncounter("All players fighting Maxor have been defeated. The encounter has ended.");
    }

    private static void abortEncounter(String message) {
        if (maxor == null || encounterLevel == null) return;
        MinecraftServer server = encounterLevel.getServer();
        restoreLaserHealth(server);
        for (UUID uuid : MINIONS) {
            Entity entity = encounterLevel.getEntity(uuid);
            if (entity != null) entity.discard();
        }
        if (maxor.isAlive()) maxor.discard();
        BOSS_BAR.removeAllPlayers();

        ServerPlayer owner = testMode ? null : server.getPlayerList().getPlayer(altarOwner);
        if (owner != null) {
            ItemStack altar = new ItemStack(ModItems.WITHER_ALTAR);
            if (!owner.getInventory().add(altar)) owner.drop(altar, false);
            owner.sendSystemMessage(Component.literal("The encounter failed. Your Wither Altar has been returned.")
                    .withStyle(style -> style.withColor(0xFF5555)));
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        BossArena.restore();
        clearEncounter();
    }

    private static void restoreLaserHealth(MinecraftServer server) {
        for (UUID uuid : LASER_AFFECTED) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                SkyblockPlayerState.clearMaxorLaserStacksFor(uuid);
            }
        }
        LASER_AFFECTED.clear();
    }

    /** Also called by direct-damage abilities that bypass hurtServer. */
    public static void recordDirectDamage(LivingEntity target, ServerPlayer attacker, double entityDamage) {
        if (entityDamage <= 0.0D) return;
        if (maxor != null && target == maxor) {
            recordDamage(attacker, entityDamage);
            return;
        }
        if (NecronEncounter.isNecron(target)) {
            NecronEncounter.recordDirectDamage(target, attacker, entityDamage);
            return;
        }
        if (GoldorEncounter.isGoldor(target)) {
            GoldorEncounter.recordDirectDamage(target, attacker, entityDamage);
            return;
        }
        StormEncounter.recordDirectDamage(target, attacker, entityDamage);
    }

    private static void recordDamage(ServerPlayer player, double entityDamage) {
        PLAYER_DAMAGE.merge(player.getUUID(), entityDamage * BOSS_HP_PER_ENTITY_HP, Double::sum);
        PARTICIPANTS.add(player.getUUID());
    }

    private static void clearEncounter() {
        if (stationaryLaser != null) stationaryLaser.discard();
        stationaryLaser = null;
        stationaryLaserActive = false;
        if (encounterLevel != null) {
            for (UUID id : challengeCrystals) {
                Entity crystal = encounterLevel.getEntity(id);
                if (crystal != null) crystal.discard();
            }
        }
        challengeCrystals.clear();
        crystalChallengeTicks = 0;
        maxorVulnerable = true;
        laserChargeTicks = 0;
        laserBeamTicks = 0;
        laserShotStart = null;
        laserShotEnd = null;
        maxor = null;
        testMode = false;
        lastKnownHealth = 0.0F;
        encounterLevel = null;
        altarOwner = null;
        minionSpawnTimer = 0;
        PLAYER_DAMAGE.clear();
        PARTICIPANTS.clear();
        LASER_AFFECTED.clear();
        MINIONS.clear();
    }
}
