package dev.hypixelcustomrecipe;

import com.mojang.math.Transformation;
import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenMobs;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class NecronEncounter {
    private static final double MAX_HP = 500_000_000.0D;
    private static final double ENTITY_HP = 1_024.0D;
    private static final double HP_PER_ENTITY_HP = MAX_HP / ENTITY_HP;
    private static final int HALF_ARENA = 50;
    private static final int WALL_HEIGHT = 12;
    private static final int LASER_DURATION = 240;
    private static final int TNT_COUNT = 50;
    private static final int DANGER_WARNING_TICKS = 100;
    private static final int DANGER_LIFETIME_TICKS = 1_300;

    private static WitherBoss necron;
    private static ServerLevel level;
    private static BlockPos center;
    private static boolean testMode;
    private static boolean laserActive;
    private static int laserTicks;
    private static double laserAngle;
    private static Guardian laserGuardian;
    private static Display.BlockDisplay laserBeamDisplay;
    private static Display.BlockDisplay safeZoneDisplay;
    private static final List<Display.BlockDisplay> CLOSING_WALL_DISPLAYS = new ArrayList<>();
    private static final List<Display.BlockDisplay> DANGER_ZONE_DISPLAYS = new ArrayList<>();
    private static final List<Display.BlockDisplay> FLAME_WALL_DISPLAYS = new ArrayList<>();
    private static int dangerDisplayColor = -1;
    private static ArmorStand laserTarget;
    private static int attack1Timer, attack2Timer, attack3Timer, attack4Timer;
    private static int attack5Timer, attack6Timer, attack7Timer;
    private static int attack1Ticks, attack4Ticks, attack6Ticks, attack7Ticks;
    private static double safeX, safeZ, safeRadius;
    private static int dangerMinX, dangerMaxX, dangerMinZ, dangerMaxZ;
    private static int flameSide, flameGap;
    private static double flameOffset;
    private static int skullFireTicks;
    private static float lastHealth;

    private static final Map<BlockPos, BlockState> ORIGINAL_ARENA = new LinkedHashMap<>();
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final Set<UUID> DEAD_PARTICIPANTS = new HashSet<>();
    private static final Set<UUID> MINIONS = new HashSet<>();
    private static final Set<UUID> PROJECTILES = new HashSet<>();
    private static final Set<UUID> FLAME_HIT_PLAYERS = new HashSet<>();
    private static final List<TntBomb> BOMBS = new ArrayList<>();
    private static final ServerBossEvent BAR = new ServerBossEvent(UUID.randomUUID(),
            Component.literal("Necron"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    private NecronEncounter() {}

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (necron != null && entity == necron
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && PARTICIPANTS.contains(player.getUUID())) {
                DEAD_PARTICIPANTS.add(player.getUUID());
                if (level != null && allFightersDead(player.level().getServer())) {
                    finish(false);
                    return;
                }
            }
            if (necron != null && entity == necron) finish(true);
        });
        ServerTickEvents.END_SERVER_TICK.register(NecronEncounter::tick);
    }

    public static boolean isActive() { return necron != null && necron.isAlive(); }
    public static boolean isNecron(LivingEntity entity) { return necron != null && entity == necron; }
    public static boolean isNecronMinion(UUID entityId) { return MINIONS.contains(entityId); }
    public static double logicalMaximumHealth() { return MAX_HP; }
    public static double toEntityDamage(double logicalDamage) { return logicalDamage / HP_PER_ENTITY_HP; }
    public static double toLogicalDamage(double entityDamage) { return entityDamage * HP_PER_ENTITY_HP; }
    public static boolean isLaserActive() { return isActive() && laserActive; }

    public static boolean isInsideArenaForVoidridden(double x, double z) {
        if (!isActive() || center == null) return true;
        return x >= center.getX() - HALF_ARENA + 2.0D
                && x < center.getX() + HALF_ARENA - 2.0D
                && z >= center.getZ() - HALF_ARENA + 2.0D
                && z < center.getZ() + HALF_ARENA - 2.0D;
    }

    public static boolean canPlaceBlock(ServerPlayer player, BlockPos pos) {
        if (!isActive() || player.level() != level || !insideArena(pos)) return true;
        return !laserActive;
    }

    private static boolean insideArena(BlockPos pos) {
        return pos.getX() >= center.getX() - HALF_ARENA
                && pos.getX() < center.getX() + HALF_ARENA
                && pos.getZ() >= center.getZ() - HALF_ARENA
                && pos.getZ() < center.getZ() + HALF_ARENA
                && pos.getY() > center.getY()
                && pos.getY() <= center.getY() + WALL_HEIGHT;
    }

    public static boolean summonForTesting(ServerPlayer player) {
        if (isActive() || MaxorEncounter.isActive() || StormEncounter.isActive()
                || GoldorEncounter.isActive()) return false;
        return start(player, (ServerLevel) player.level(), true);
    }

    private static boolean start(ServerPlayer starter, ServerLevel world, boolean testing) {
        center = starter.blockPosition().immutable();
        level = world;
        buildArena();

        WitherBoss boss = EntityTypes.WITHER.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) {
            restoreArena();
            clearState();
            return false;
        }
        boss.setPos(center.getX() + 0.5D, center.getY() + 5.0D, center.getZ() + 0.5D);
        boss.setCustomName(Component.literal("Necron").withStyle(s -> s.withColor(0xAA0000).withBold(true)));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        boss.setNoAi(true);
        boss.setNoGravity(true);
        var maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(ENTITY_HP);
        boss.setHealth((float) ENTITY_HP);
        if (!world.addFreshEntity(boss)) {
            restoreArena();
            clearState();
            return false;
        }

        necron = boss;
        testMode = testing;
        lastHealth = (float) ENTITY_HP;
        PARTICIPANTS.clear();
        DEAD_PARTICIPANTS.clear();
        MINIONS.clear();
        PROJECTILES.clear();
        FLAME_HIT_PLAYERS.clear();
        BOMBS.clear();
        attack1Timer = random(225, 338);
        attack2Timer = random(150, 263);
        attack3Timer = random(150, 300);
        attack4Timer = random(300, 375);
        attack5Timer = random(150, 300);
        attack6Timer = 450;
        attack7Timer = random(100, 160);
        laserActive = false;
        laserTicks = 0;
        attack1Ticks = attack4Ticks = attack6Ticks = attack7Ticks = 0;
        laserAngle = 0.0D;
        BAR.removeAllPlayers();
        BAR.setProgress(1.0F);
        BAR.setName(Component.literal("Necron: 500,000,000 / 500,000,000 HP"));
        BAR.addPlayer(starter);

        starter.teleportTo(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
        starter.sendSystemMessage(Component.literal(testing
                ? "NECRON TEST SUMMONED — 500,000,000 HP!"
                : "NECRON HAS AWAKENED — 500,000,000 HP!")
                .withStyle(s -> s.withColor(0xAA0000).withBold(true)));
        starter.sendSystemMessage(Component.literal(
                "The 100x100 bedrock arena is sealed. No building is allowed during the fight.")
                .withStyle(s -> s.withColor(0xFF5555)));
        world.playSound(null, center, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.5F);
        return true;
    }

    private static int random(int min, int max) {
        return min + level.getRandom().nextInt(max - min + 1);
    }

    private static void buildArena() {
        ORIGINAL_ARENA.clear();
        int minX = center.getX() - HALF_ARENA;
        int maxX = center.getX() + HALF_ARENA - 1;
        int minZ = center.getZ() - HALF_ARENA;
        int maxZ = center.getZ() + HALF_ARENA - 1;
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
                            ORIGINAL_ARENA.putIfAbsent(immutable, original);
                            level.setBlock(immutable, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    private static void saveAndPlaceBedrock(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        ORIGINAL_ARENA.putIfAbsent(immutable, level.getBlockState(immutable));
        level.setBlock(immutable, Blocks.BEDROCK.defaultBlockState(), 3);
    }

    private static void restoreArena() {
        if (level == null) return;
        for (Map.Entry<BlockPos, BlockState> entry : ORIGINAL_ARENA.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        ORIGINAL_ARENA.clear();
    }

    private static void tick(MinecraftServer server) {
        if (necron == null || level == null || !necron.isAlive() || necron.isRemoved()) return;
        if (allFightersDead(server)) {
            finish(false);
            return;
        }

        float hp = necron.getHealth();
        if (hp > lastHealth) necron.setHealth(lastHealth);
        else lastHealth = hp;

        ServerPlayer target = nearestPlayer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != level) {
                BAR.removePlayer(player);
                continue;
            }
            if (player.distanceToSqr(Vec3.atCenterOf(center)) <= 110.0D * 110.0D) BAR.addPlayer(player);
            else BAR.removePlayer(player);
            if (player.isFallFlying()) player.stopFallFlying();
        }

        MINIONS.removeIf(id -> {
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive()) return true;
            if (entity instanceof net.minecraft.world.entity.Mob mob && target != null) mob.setTarget(target);
            return false;
        });
        PROJECTILES.removeIf(id -> {
            Entity entity = level.getEntity(id);
            return entity == null || !entity.isAlive();
        });

        if (laserActive) {
            tickLaser();
        } else {
            tickOtherAttacks(target);
        }

        float progress = Math.max(0.0F, Math.min(1.0F, necron.getHealth() / (float) ENTITY_HP));
        BAR.setProgress(progress);
        long shown = Math.max(0L, Math.round(progress * MAX_HP));
        BAR.setName(Component.literal("Necron: " + String.format(Locale.ROOT, "%,d", shown)
                + " / 500,000,000 HP"));
    }

    private static ServerPlayer nearestPlayer() {
        ServerPlayer nearest = null;
        double best = 100.0D * 100.0D;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive() || !insideArena(player.blockPosition())) continue;
            double distance = player.distanceToSqr(Vec3.atCenterOf(center));
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void tickOtherAttacks(ServerPlayer target) {
        tickAttack1();
        tickAttack2();
        tickAttack4();
        tickAttack6();
        tickAttack7(target);
        if (attack1Timer > 0) attack1Timer--;
        if (attack2Timer > 0) attack2Timer--;
        if (attack4Timer > 0) attack4Timer--;
        if (attack5Timer > 0) attack5Timer--;
        if (attack6Timer > 0) attack6Timer--;

        if (attack3Timer > 0) attack3Timer--;
        if (attack7Ticks <= 0 && attack7Timer > 0) attack7Timer--;
        if (attack3Timer <= 0 && attack4Ticks == 0) {
            beginLaser();
            return;
        }

        if (attack4Ticks == 0 && attack4Timer <= 0) beginAttack4();
        if (attack6Ticks == 0 && attack6Timer <= 0) beginAttack6();
        if (attack1Ticks == 0 && attack1Timer <= 0
                && attack4Ticks == 0 && attack6Ticks == 0) beginAttack1();
        if (BOMBS.isEmpty() && attack2Timer <= 0
                && attack6Ticks == 0) beginAttack2();
        if (attack5Timer > 0) attack5Timer--;
        if (attack5Timer <= 0) {
            spawnVoidriddenWave(target);
            attack5Timer = random(150, 300);
        }
        if (attack7Ticks == 0 && attack7Timer <= 0) beginAttack7();
    }

    // Attack 1 — a smooth end-rod ring closes toward a five-block safe circle at 5 blocks/second.
    private static void beginAttack1() {
        // Keep the entire safe circle outside an active Danger Zone.
        do {
            safeX = center.getX() + 0.5D + level.getRandom().nextDouble() * 70.0D - 35.0D;
            safeZ = center.getZ() + 0.5D + level.getRandom().nextDouble() * 70.0D - 35.0D;
        } while (overlapsDangerZone(safeX, safeZ, 2.5D));
        double farX = Math.max(Math.abs(safeX - (center.getX() - HALF_ARENA)),
                Math.abs(safeX - (center.getX() + HALF_ARENA)));
        double farZ = Math.max(Math.abs(safeZ - (center.getZ() - HALF_ARENA)),
                Math.abs(safeZ - (center.getZ() + HALF_ARENA)));
        safeRadius = Math.sqrt(farX * farX + farZ * farZ) + 2.0D;
        attack1Ticks = (int) Math.ceil((safeRadius - 2.5D) / 0.25D);
        clearDisplayList(CLOSING_WALL_DISPLAYS);
        for (int i = 0; i < 16; i++) {
            Display.BlockDisplay segment = createBlockDisplay(Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
                    safeX, center.getY() + 5.2D, safeZ, 1.0F, 10.0F, 0.18F, new Quaternionf());
            if (segment != null) CLOSING_WALL_DISPLAYS.add(segment);
        }
        safeZoneDisplay = createBlockDisplay(Blocks.LIME_STAINED_GLASS.defaultBlockState(),
                safeX, center.getY() + 1.04D, safeZ, 5.0F, 0.08F, 5.0F, new Quaternionf());
        attack1Timer = random(225, 338);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON: EVAPORATION! Reach the 5-block safe circle!")
                .withStyle(s -> s.withColor(0x55FFFF).withBold(true)), false);
        level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.4F);
    }

    private static boolean overlapsDangerZone(double x, double z, double radius) {
        if (attack4Ticks <= 0) return false;
        double closestX = Math.max(dangerMinX, Math.min(x, dangerMaxX + 1.0D));
        double closestZ = Math.max(dangerMinZ, Math.min(z, dangerMaxZ + 1.0D));
        double dx = x - closestX;
        double dz = z - closestZ;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean circleOverlapsRectangle(double x, double z, double radius,
                                                       double minX, double maxX, double minZ, double maxZ) {
        double closestX = Math.max(minX, Math.min(x, maxX));
        double closestZ = Math.max(minZ, Math.min(z, maxZ));
        double dx = x - closestX;
        double dz = z - closestZ;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean isInsideClosingWallSafeZone(ServerPlayer player) {
        if (attack1Ticks <= 0) return false;
        double dx = player.getX() - safeX;
        double dz = player.getZ() - safeZ;
        return dx * dx + dz * dz <= 2.5D * 2.5D;
    }

    private static void tickAttack1() {
        if (attack1Ticks <= 0) return;
        double previous = safeRadius;
        safeRadius = Math.max(2.5D, safeRadius - 0.25D);
        updateClosingWallDisplays();
        AABB arena = new AABB(center.getX() - HALF_ARENA, center.getY() + 1,
                center.getZ() - HALF_ARENA, center.getX() + HALF_ARENA,
                center.getY() + WALL_HEIGHT, center.getZ() + HALF_ARENA);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, arena)) {
            double distance = Math.sqrt(player.distanceToSqr(safeX, player.getY(), safeZ));
            if (!isInsideClosingWallSafeZone(player) && distance > 2.5D
                    && distance >= safeRadius - 0.9D && distance <= previous + 0.9D) {
                kill(player, "Necron's closing wall");
            }
        }
        attack1Ticks--;
        if (attack1Timer > 0) attack1Timer--;
        if (attack1Ticks <= 0) {
            clearDisplayList(CLOSING_WALL_DISPLAYS);
            discardDisplay(safeZoneDisplay);
            safeZoneDisplay = null;
            level.playSound(null, center, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 1.2F);
        }
    }

    // Attack 2 — fifty falling TNT displays; the blast is custom and cannot destroy the arena.
    private static void beginAttack2() {
        for (int i = 0; i < TNT_COUNT; i++) {
            double x = center.getX() - HALF_ARENA + 1.0D + level.getRandom().nextDouble() * 98.0D;
            double z = center.getZ() - HALF_ARENA + 1.0D + level.getRandom().nextDouble() * 98.0D;
            double y = center.getY() + 22.0D + level.getRandom().nextDouble() * 5.0D;
            Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.COMMAND);
            if (display != null) {
                display.setItemStack(new ItemStack(Items.TNT));
                display.setItemTransform(ItemDisplayContext.FIXED);
                display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                        new Vector3f(0.75F), new Quaternionf()));
                display.setNoGravity(true);
                display.setPos(x, y, z);
                level.addFreshEntity(display);
            }
            Display.BlockDisplay marker = createBlockDisplay(Blocks.RED_STAINED_GLASS.defaultBlockState(),
                    x, center.getY() + 1.04D, z, 10.0F, 0.06F, 10.0F, new Quaternionf());
            BOMBS.add(new TntBomb(x, z, y, display, marker, 0));
        }
        attack2Timer = random(150, 263);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON: TNT RAIN! Keep moving!")
                .withStyle(s -> s.withColor(0xFF5555).withBold(true)), false);
        level.playSound(null, center, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 3.0F, 0.8F);
    }

    private static void tickAttack2() {
        if (attack2Timer > 0) attack2Timer--;
        Iterator<TntBomb> iterator = BOMBS.iterator();
        while (iterator.hasNext()) {
            TntBomb bomb = iterator.next();
            bomb.age++;
            bomb.y = Math.max(center.getY() + 1.0D, bomb.y - 0.45D);
            if (bomb.display != null && !bomb.display.isRemoved()) {
                bomb.display.setPos(bomb.x, bomb.y, bomb.z);
            }

            if (bomb.y <= center.getY() + 1.0D || bomb.age >= 80) {
                if (bomb.display != null) bomb.display.discard();
                discardDisplay(bomb.marker);
                detonateBomb(bomb.x, bomb.z);
                iterator.remove();
            }
        }
    }

    private static void detonateBomb(double x, double z) {
        double y = center.getY() + 1.1D;
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.0F, 0.85F);
        AABB area = new AABB(x - 5.0D, center.getY(), z - 5.0D,
                x + 5.0D, center.getY() + WALL_HEIGHT, z + 5.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz > 25.0D || !player.isAlive()) continue;
            SkyblockPlayerState state = SkyblockPlayerState.get(player);
            state.markCombat();
            if (state.applyNecronCorruptDamage(1_000.0F)) {
                player.setHealth(0.0F);
                player.die(level.damageSources().magic());
            }
            DamageNumberHandler.show(level, player, 1_000.0F, false);
            player.sendSystemMessage(Component.literal(
                    "CORRUPT DAMAGE! Your healing is capped until you die or Necron is defeated.")
                    .withStyle(s -> s.withColor(0xAA00AA)));
        }
    }

    // Attack 3 — Laser: uses the actual Guardian beam and sweeps around the center like a jump rope.
    private static void beginLaser() {
        laserActive = true;
        laserTicks = LASER_DURATION;
        attack3Timer = random(150, 300);
        laserAngle = 0.0D;
        if (necron != null) {
            necron.setPos(center.getX() + 0.5D, center.getY() + 5.0D, center.getZ() + 0.5D);
            necron.setDeltaMovement(Vec3.ZERO);
        }
        laserGuardian = EntityTypes.GUARDIAN.create(level, EntitySpawnReason.MOB_SUMMONED);
        laserTarget = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        if (laserGuardian != null && laserTarget != null) {
            laserGuardian.setPos(center.getX() + 0.5D, center.getY() + 1.2D, center.getZ() + 0.5D);
            laserGuardian.setNoGravity(true);
            laserGuardian.setInvulnerable(true);
            laserGuardian.setPersistenceRequired();
            laserTarget.setInvisible(true);
            laserTarget.setNoGravity(true);
            laserTarget.setInvulnerable(true);
            laserTarget.setPos(center.getX() + 48.0D, center.getY() + 1.6D, center.getZ() + 0.5D);
            level.addFreshEntity(laserTarget);
            level.addFreshEntity(laserGuardian);
            laserGuardian.setTarget(laserTarget);
        }
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON: LASER! JUMP OVER THE ROTATING BEAM! Building is disabled.")
                .withStyle(s -> s.withColor(0xFF5555).withBold(true)), false);
        level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0F, 0.55F);
    }

    private static void tickLaser() {
        if (attack3Timer > 0) attack3Timer--;
        laserAngle += Math.PI * 2.0D / 160.0D;
        double radius = 48.0D;
        double endX = center.getX() + 0.5D + Math.cos(laserAngle) * radius;
        double endZ = center.getZ() + 0.5D + Math.sin(laserAngle) * radius;
        double beamY = center.getY() + 1.6D;
        if (laserTarget != null && !laserTarget.isRemoved()) {
            laserTarget.setPos(endX, beamY, endZ);
        }
        if (laserGuardian != null && !laserGuardian.isRemoved()) {
            laserGuardian.setPos(center.getX() + 0.5D, center.getY() + 0.1D, center.getZ() + 0.5D);
            laserGuardian.setTarget(laserTarget);
        }

        double midX = center.getX() + 0.5D + Math.cos(laserAngle) * radius * 0.5D;
        double midZ = center.getZ() + 0.5D + Math.sin(laserAngle) * radius * 0.5D;
        if (laserBeamDisplay == null || laserBeamDisplay.isRemoved()) {
            laserBeamDisplay = createBlockDisplay(Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
                    midX, beamY, midZ, 0.14F, 0.14F, (float) radius, new Quaternionf().rotationY((float) laserAngle));
        } else {
            laserBeamDisplay.setPos(midX, beamY, midZ);
            laserBeamDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotationY((float) laserAngle),
                    new Vector3f(0.14F, 0.14F, (float) radius), new Quaternionf()));
        }

        // Damage follows the same radial path as the rendered Guardian beam.
        Vec3 start = new Vec3(center.getX() + 0.5D, beamY, center.getZ() + 0.5D);
        Vec3 end = new Vec3(endX, beamY, endZ);
        AABB area = new AABB(start, end).inflate(1.1D, 1.5D, 1.1D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (player.getY() > beamY + 0.45D) continue;
            double distance = distanceToSegment(player.position().add(0.0D, 0.9D, 0.0D), start, end);
            if (distance <= 0.85D) {
                SkyblockPlayerState state = SkyblockPlayerState.get(player);
                float damage = (float) (state.maximumHealth() * 0.20D / 10.0D);
                state.markCombat();
                if (state.applyTrueDamage(damage)) {
                    player.setHealth(0.0F);
                    player.die(level.damageSources().magic());
                }
                DamageNumberHandler.show(level, player, damage, false);
            }
        }

        laserTicks--;
        if (laserTicks <= 0) {
            laserActive = false;
            if (laserGuardian != null) laserGuardian.discard();
            if (laserTarget != null) laserTarget.discard();
            laserGuardian = null;
            laserTarget = null;
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                    "Necron's laser has ended. The other attacks resume.")
                    .withStyle(s -> s.withColor(0x55FFFF)), false);
            level.playSound(null, center, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.8F);
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-8D) return point.distanceTo(start);
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(segment) / lengthSquared));
        return point.distanceTo(start.add(segment.scale(t)));
    }

    // Attack 4 — a yellow/orange/red outline warns about a lethal 50x50 square.
    private static void beginAttack4() {
        // Never place a new Danger Zone over the active Closing Wall safe circle.
        do {
            dangerMinX = center.getX() - HALF_ARENA + level.getRandom().nextInt(51);
            dangerMinZ = center.getZ() - HALF_ARENA + level.getRandom().nextInt(51);
            dangerMaxX = dangerMinX + 49;
            dangerMaxZ = dangerMinZ + 49;
        } while (attack1Ticks > 0 && circleOverlapsRectangle(
                safeX, safeZ, 2.5D, dangerMinX, dangerMaxX + 1.0D, dangerMinZ, dangerMaxZ + 1.0D));
        attack4Ticks = DANGER_LIFETIME_TICKS;
        clearDisplayList(DANGER_ZONE_DISPLAYS);
        dangerDisplayColor = -1;
        double zoneMidX = (dangerMinX + dangerMaxX + 1.0D) * 0.5D;
        double zoneMidZ = (dangerMinZ + dangerMaxZ + 1.0D) * 0.5D;
        double zoneY = center.getY() + 1.08D;
        for (int side = 0; side < 4; side++) {
            boolean alongX = side < 2;
            double px = alongX ? zoneMidX : (side == 2 ? dangerMinX + 0.5D : dangerMaxX + 0.5D);
            double pz = alongX ? (side == 0 ? dangerMinZ + 0.5D : dangerMaxZ + 0.5D) : zoneMidZ;
            Display.BlockDisplay edge = createBlockDisplay(Blocks.YELLOW_CONCRETE.defaultBlockState(),
                    px, zoneY, pz, alongX ? 50.0F : 0.15F, 0.08F, alongX ? 0.15F : 50.0F, new Quaternionf());
            if (edge != null) DANGER_ZONE_DISPLAYS.add(edge);
        }
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON: DANGER ZONE! Leave the outlined area before it turns red!")
                .withStyle(s -> s.withColor(0xFFFF55).withBold(true)), false);
    }

    private static void tickAttack4() {
        if (attack4Ticks <= 0) return;
        int elapsed = DANGER_LIFETIME_TICKS - attack4Ticks;
        int color = elapsed < 60 ? 0xFFFF00 : elapsed < DANGER_WARNING_TICKS ? 0xFF8800 : 0xFF0000;
        updateDangerZoneDisplays(color);
        if (elapsed >= DANGER_WARNING_TICKS) {
            AABB zone = new AABB(dangerMinX, center.getY() + 1, dangerMinZ,
                    dangerMaxX + 1.0D, center.getY() + WALL_HEIGHT, dangerMaxZ + 1.0D);
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, zone)) {
                if (player.isAlive() && !isInsideClosingWallSafeZone(player)) {
                    kill(player, "Necron's danger zone");
                }
            }
        }
        attack4Ticks--;
        if (attack4Ticks <= 0) {
            clearDisplayList(DANGER_ZONE_DISPLAYS);
            dangerDisplayColor = -1;
            attack4Timer = random(300, 375);
            level.playSound(null, center, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 1.0F);
        }
    }

    // Attack 5 — summon the existing custom Voidridden mobs.
    private static void spawnVoidriddenWave(ServerPlayer target) {
        spawnVoidridden(VoidriddenMobs.VOIDRIDDEN_SKELETON, 10, target);
        spawnVoidridden(VoidriddenMobs.VOIDRIDDEN_SPIDER, 8, target);
        spawnVoidridden(VoidriddenMobs.VOIDRIDDEN_ZOMBIE, 5, target);
        spawnVoidridden(VoidriddenMobs.VOIDRIDDEN_ENDERMAN, 2, target);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON: VOIDRIDDEN WAVE! 25 minions have entered the arena.")
                .withStyle(s -> s.withColor(0xAA00AA).withBold(true)), false);
    }

    private static void spawnVoidridden(EntityType<? extends net.minecraft.world.entity.Mob> type,
                                        int count, ServerPlayer target) {
        for (int i = 0; i < count; i++) {
            net.minecraft.world.entity.Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (mob == null) continue;
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 12.0D + level.getRandom().nextDouble() * 28.0D;
            double x = center.getX() + 0.5D + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5D + Math.sin(angle) * radius;
            mob.setPos(x, center.getY() + 1.0D, z);
            mob.setPersistenceRequired();
            if (target != null) mob.setTarget(target);
            if (level.addFreshEntity(mob)) MINIONS.add(mob.getUUID());
        }
    }

    // Attack 6 — a moving flame wall with a 3x3 bottom gap; each player can be hit once per wall.
    private static void beginAttack6() {
        flameSide = level.getRandom().nextInt(4);
        flameGap = level.getRandom().nextInt(41) - 20;
        flameOffset = 0.0D;
        attack6Ticks = 200;
        clearDisplayList(FLAME_WALL_DISPLAYS);
        for (int i = 0; i < 3; i++) {
            Display.BlockDisplay fire = createBlockDisplay(Blocks.FIRE.defaultBlockState(),
                    center.getX(), center.getY() + 1.0D, center.getZ(), 1.0F, 1.0F, 1.0F, new Quaternionf());
            if (fire != null) FLAME_WALL_DISPLAYS.add(fire);
        }
        FLAME_HIT_PLAYERS.clear();
        attack6Timer = 450;
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON: FLAME WALL! Find the 3x3 opening at the bottom!")
                .withStyle(s -> s.withColor(0xFF8800).withBold(true)), false);
        level.playSound(null, center, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 3.0F, 0.7F);
    }

    private static void tickAttack6() {
        if (attack6Ticks <= 0) return;
        double plane;
        boolean verticalX = flameSide < 2;
        if (flameSide == 0) plane = center.getX() - HALF_ARENA + flameOffset;
        else if (flameSide == 1) plane = center.getX() + HALF_ARENA - flameOffset;
        else if (flameSide == 2) plane = center.getZ() - HALF_ARENA + flameOffset;
        else plane = center.getZ() + HALF_ARENA - flameOffset;

        double yBottom = center.getY() + 1.0D;
        updateFlameWallDisplays(plane, verticalX, yBottom);

        AABB wall = verticalX
                ? new AABB(plane - 1.0D, center.getY() + 1, center.getZ() - HALF_ARENA,
                    plane + 1.0D, center.getY() + 12, center.getZ() + HALF_ARENA)
                : new AABB(center.getX() - HALF_ARENA, center.getY() + 1, plane - 1.0D,
                    center.getX() + HALF_ARENA, center.getY() + 12, plane + 1.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, wall)) {
            if (FLAME_HIT_PLAYERS.contains(player.getUUID())) continue;
            double along = verticalX ? player.getZ() - center.getZ() : player.getX() - center.getX();
            boolean inGap = Math.abs(along - flameGap) < 1.5D
                    && player.getY() <= center.getY() + 3.0D;
            if (!inGap) {
                FLAME_HIT_PLAYERS.add(player.getUUID());
                SkyblockPlayerState.get(player).addNecronFlameWallStack();
                player.sendSystemMessage(Component.literal(
                        "FLAME WALL HIT! Your maximum Health has been reduced by 10% for this fight.")
                        .withStyle(s -> s.withColor(0xFF5555).withBold(true)));
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        }

        flameOffset += 0.5D;
        attack6Ticks--;
        if (attack6Ticks <= 0) {
            FLAME_HIT_PLAYERS.clear();
            clearDisplayList(FLAME_WALL_DISPLAYS);
        }
    }

    // Attack 7 — a ten-second stream of real Wither Skulls aimed at the current player target.
    private static void beginAttack7() {
        attack7Ticks = 200;
        skullFireTicks = 0;
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "NECRON: WITHER SKULL BARRAGE! Keep moving!")
                .withStyle(s -> s.withColor(0xAA00AA).withBold(true)), false);
        level.playSound(null, center, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 3.0F, 0.65F);
    }

    private static void tickAttack7(ServerPlayer target) {
        if (attack7Ticks <= 0) {
            return;
        }
        if (target != null) {
            Vec3 start = necron.position().add(0.0D, 1.6D, 0.0D);
            Vec3 direction = target.getEyePosition().subtract(start).normalize();
            if (++skullFireTicks >= 5) {
                skullFireTicks = 0;
                WitherSkull skull = EntityTypes.WITHER_SKULL.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (skull != null) {
                    skull.setOwner(necron);
                    skull.setDangerous(false);
                    skull.setPos(start.x, start.y, start.z);
                    skull.setDeltaMovement(direction.scale(1.15D));
                    if (level.addFreshEntity(skull)) PROJECTILES.add(skull.getUUID());
                }
            }
        }
        attack7Ticks--;
        if (attack7Ticks <= 0) attack7Timer = random(100, 160);
    }

    private static void kill(ServerPlayer player, String cause) {
        if (!player.isAlive() || isInsideClosingWallSafeZone(player)) return;
        SkyblockPlayerState state = SkyblockPlayerState.get(player);
        state.applyTrueDamage(state.maximumHealth() / 10.0F + 1.0F);
        player.setHealth(0.0F);
        player.die(level.damageSources().magic());
        player.sendSystemMessage(Component.literal("You were killed by " + cause + ".")
                .withStyle(s -> s.withColor(0xFF5555)));
    }

    public static void recordDirectDamage(LivingEntity target, ServerPlayer attacker, double entityDamage) {
        if (necron == null || target != necron || entityDamage <= 0.0D) return;
        PARTICIPANTS.add(attacker.getUUID());
    }

    private static boolean allFightersDead(MinecraftServer server) {
        if (PARTICIPANTS.isEmpty()) return false;
        for (UUID id : PARTICIPANTS) {
            ServerPlayer fighter = server.getPlayerList().getPlayer(id);
            if (!DEAD_PARTICIPANTS.contains(id) && fighter != null && fighter.isAlive()) return false;
        }
        return true;
    }

    private static void finish(boolean defeated) {
        if (level == null) return;
        BAR.removeAllPlayers();
        if (!defeated && necron != null) necron.discard();
        if (defeated && necron != null) {
            AABB deathArea = new AABB(necron.blockPosition()).inflate(6.0D);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, deathArea)) {
                if (item.getItem().is(Items.NETHER_STAR)) item.discard();
            }
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, deathArea)) orb.discard();
        }
        for (UUID id : MINIONS) {
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
        }
        for (UUID id : PROJECTILES) {
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
        }
        for (TntBomb bomb : BOMBS) {
            if (bomb.display != null) bomb.display.discard();
            discardDisplay(bomb.marker);
        }
        discardDisplay(laserBeamDisplay);
        discardDisplay(safeZoneDisplay);
        clearDisplayList(CLOSING_WALL_DISPLAYS);
        clearDisplayList(DANGER_ZONE_DISPLAYS);
        clearDisplayList(FLAME_WALL_DISPLAYS);
        if (laserGuardian != null) laserGuardian.discard();
        if (laserTarget != null) laserTarget.discard();
        SkyblockPlayerState.clearNecronEncounterEffectsForAllPlayers();
        if (defeated) {
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("NECRON HAS BEEN DEFEATED! The arena is disappearing.")
                            .withStyle(s -> s.withColor(0x55FF55).withBold(true)), false);
            level.playSound(null, center, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 4.0F, 0.55F);
        } else {
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("All players fighting Necron have been defeated. The encounter has ended.")
                            .withStyle(s -> s.withColor(0xFF5555)), false);
        }
        restoreArena();
        clearState();
    }

    private static void clearState() {
        necron = null;
        level = null;
        center = null;
        testMode = false;
        laserActive = false;
        laserTicks = 0;
        laserGuardian = null;
        laserTarget = null;
        laserBeamDisplay = null;
        safeZoneDisplay = null;
        dangerDisplayColor = -1;
        attack1Ticks = attack4Ticks = attack6Ticks = attack7Ticks = 0;
        attack1Timer = attack2Timer = attack3Timer = attack4Timer = 0;
        attack5Timer = attack6Timer = attack7Timer = 0;
        PARTICIPANTS.clear();
        MINIONS.clear();
        PROJECTILES.clear();
        FLAME_HIT_PLAYERS.clear();
        BOMBS.clear();
        ORIGINAL_ARENA.clear();
    }


    private static Display.BlockDisplay createBlockDisplay(BlockState state, double x, double y, double z,
                                                              float sx, float sy, float sz, Quaternionf rotation) {
        Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
        if (display == null) return null;
        display.setBlockState(state);
        display.setTransformation(new Transformation(new Vector3f(), rotation, new Vector3f(sx, sy, sz), new Quaternionf()));
        display.setNoGravity(true);
        display.setPos(x, y, z);
        if (!level.addFreshEntity(display)) return null;
        return display;
    }

    private static void discardDisplay(Display.BlockDisplay display) {
        if (display != null && !display.isRemoved()) display.discard();
    }

    private static void clearDisplayList(List<Display.BlockDisplay> displays) {
        for (Display.BlockDisplay display : displays) discardDisplay(display);
        displays.clear();
    }

    private static void updateClosingWallDisplays() {
        int count = CLOSING_WALL_DISPLAYS.size();
        if (count == 0) return;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double arcLength = Math.max(1.0D, Math.PI * 2.0D * safeRadius / count);
            Display.BlockDisplay display = CLOSING_WALL_DISPLAYS.get(i);
            display.setPos(safeX + Math.cos(angle) * safeRadius, center.getY() + 5.2D,
                    safeZ + Math.sin(angle) * safeRadius);
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotationY((float) (Math.PI / 2.0D - angle)),
                    new Vector3f((float) arcLength, 10.0F, 0.18F), new Quaternionf()));
        }
    }

    private static void updateDangerZoneDisplays(int color) {
        if (dangerDisplayColor == color) return;
        dangerDisplayColor = color;
        net.minecraft.world.level.block.Block block = color == 0xFFFF00 ? Blocks.YELLOW_CONCRETE
                : color == 0xFF8800 ? Blocks.ORANGE_CONCRETE : Blocks.RED_CONCRETE;
        for (Display.BlockDisplay display : DANGER_ZONE_DISPLAYS) display.setBlockState(block.defaultBlockState());
    }

    private static void updateFlameWallDisplays(double plane, boolean verticalX, double yBottom) {
        if (FLAME_WALL_DISPLAYS.size() != 3) return;
        double gapStart = flameGap - 1.5D;
        double gapEnd = flameGap + 1.5D;
        double leftWidth = Math.max(0.1D, gapStart + 49.0D);
        double rightWidth = Math.max(0.1D, 49.0D - gapEnd);
        setFlameDisplay(FLAME_WALL_DISPLAYS.get(0), plane,
                (verticalX ? center.getZ() : center.getX()) + (-49.0D + gapStart) * 0.5D,
                yBottom + 1.5D, verticalX, leftWidth, 3.0F);
        setFlameDisplay(FLAME_WALL_DISPLAYS.get(1), plane,
                (verticalX ? center.getZ() : center.getX()) + (gapEnd + 49.0D) * 0.5D,
                yBottom + 1.5D, verticalX, rightWidth, 3.0F);
        setFlameDisplay(FLAME_WALL_DISPLAYS.get(2), plane,
                verticalX ? center.getZ() : center.getX(), yBottom + 7.0D, verticalX, 98.0D, 8.0F);
    }

    private static void setFlameDisplay(Display.BlockDisplay display, double plane, double alongCenter,
                                        double y, boolean verticalX, double width, float height) {
        double x = verticalX ? plane : alongCenter;
        double z = verticalX ? alongCenter : plane;
        display.setPos(x, y, z);
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                verticalX ? new Vector3f(0.18F, height, (float) width)
                        : new Vector3f((float) width, height, 0.18F), new Quaternionf()));
    }

    private static final class TntBomb {
        final double x;
        final double z;
        final Display.ItemDisplay display;
        final Display.BlockDisplay marker;
        double y;
        int age;

        TntBomb(double x, double z, double y, Display.ItemDisplay display, Display.BlockDisplay marker, int age) {
            this.x = x;
            this.z = z;
            this.y = y;
            this.display = display;
            this.marker = marker;
            this.age = age;
        }
    }
}