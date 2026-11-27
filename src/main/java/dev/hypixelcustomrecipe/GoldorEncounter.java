package dev.hypixelcustomrecipe;

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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageTypeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Goldor's chase-and-sword-slam encounter. */
public final class GoldorEncounter {
    private static final double MAX_HEALTH = 25_000_000.0D;
    private static final double ENTITY_HEALTH = 1_024.0D;
    private static final double HP_SCALE = MAX_HEALTH / ENTITY_HEALTH;
    private static final int WARNING_TICKS = 40;
    private static final int SWORD_LIFETIME = 600;
    private static final int ATTACK_INTERVAL = 100;

    private static WitherBoss goldor;
    private static ServerLevel encounterLevel;
    private static boolean testMode;
    private static int attackTimer;
    private static float lastHealth;
    private static final List<PendingSword> PENDING = new ArrayList<>();
    private static final List<ActiveSword> ACTIVE = new ArrayList<>();
    private static final ServerBossEvent BAR = new ServerBossEvent(
            UUID.randomUUID(), Component.literal("Goldor"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    private GoldorEncounter() {}

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (goldor != null && entity == goldor
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (goldor != null && entity == goldor) finish();
        });
        ServerTickEvents.END_SERVER_TICK.register(GoldorEncounter::tick);
    }

    public static boolean isGoldor(net.minecraft.world.entity.LivingEntity entity) {\n        return goldor != null && entity == goldor;\n    }\n\n    public static void recordDirectDamage(net.minecraft.world.entity.LivingEntity target, double entityDamage) {\n        if (goldor == null || target != goldor || entityDamage <= 0.0D) return;\n        float next = (float) Math.max(0.0D, goldor.getHealth() - entityDamage);\n        goldor.setHealth(next);\n        lastHealth = next;\n        if (next <= 0.0F) goldor.die(encounterLevel.damageSources().generic());\n    }\n\n    public static boolean summonForTesting(ServerPlayer player) {
        if (goldor != null && goldor.isAlive()) return false;
        ServerLevel level = (ServerLevel) player.level();
        WitherBoss boss = EntityTypes.WITHER.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) return false;
        BlockPos spawn = player.blockPosition().relative(player.getDirection(), 5);
        boss.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        boss.setCustomName(Component.literal("Goldor").withStyle(s -> s.withColor(0xFFAA00).withBold(true)));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        var health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(ENTITY_HEALTH);
        boss.setHealth((float) ENTITY_HEALTH);
        boss.setTarget(player);
        if (!level.addFreshEntity(boss)) return false;

        goldor = boss;
        encounterLevel = level;
        testMode = true;
        attackTimer = 60;
        lastHealth = (float) ENTITY_HEALTH;
        PENDING.clear();
        ACTIVE.clear();
        BAR.setName(Component.literal("Goldor: 25,000,000 / 25,000,000 HP"));
        BAR.setProgress(1.0F);
        BAR.addPlayer(player);
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.55F);
        player.sendSystemMessage(Component.literal("GOLDOR TEST SUMMONED!")
                .withStyle(s -> s.withColor(0xFFAA00).withBold(true)));
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (goldor == null || encounterLevel == null || !goldor.isAlive() || goldor.isRemoved()) return;

        float current = goldor.getHealth();
        if (current > lastHealth) goldor.setHealth(lastHealth);
        else lastHealth = current;

        ServerPlayer target = nearestPlayer();
        if (target != null) {
            goldor.setTarget(target);
            Vec3 delta = target.position().subtract(goldor.position());
            Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
            if (horizontal.lengthSqr() > 2.25D) {
                Vec3 velocity = horizontal.normalize().scale(0.22D);
                goldor.setDeltaMovement(velocity.x, Math.max(-0.08D, Math.min(0.08D, delta.y * 0.04D)), velocity.z);
            } else {
                goldor.setDeltaMovement(Vec3.ZERO);
            }
        }

        if (--attackTimer <= 0 && target != null) {
            // Lock the player's position when the warning begins: the slam aims
            // where they were, not where they run during the warning.
            PENDING.add(new PendingSword(BlockPos.containing(target.position()), WARNING_TICKS));
            attackTimer = ATTACK_INTERVAL;
            target.sendSystemMessage(Component.literal("GOLDOR IS TARGETING YOU!")
                    .withStyle(s -> s.withColor(0xFF5555).withBold(true)));
        }

        Iterator<PendingSword> pending = PENDING.iterator();
        while (pending.hasNext()) {
            PendingSword sword = pending.next();
            renderWarning(sword.pos);
            if (--sword.ticks <= 0) {
                slam(sword.pos);
                pending.remove();
            }
        }

        Iterator<ActiveSword> active = ACTIVE.iterator();
        while (active.hasNext()) {
            ActiveSword sword = active.next();
            if (--sword.ticks <= 0) {
                active.remove();
                continue;
            }
            renderSword(sword.pos);
            AABB hitbox = new AABB(sword.pos.getX() + 0.12D, sword.pos.getY(),
                    sword.pos.getZ() + 0.12D, sword.pos.getX() + 0.88D,
                    sword.pos.getY() + 4.0D, sword.pos.getZ() + 0.88D);
            for (ServerPlayer player : encounterLevel.getEntitiesOfClass(ServerPlayer.class, hitbox)) {
                player.setHealth(0.0F);
                player.die(encounterLevel.damageSources().generic());
            }
        }

        float progress = Math.max(0.0F, Math.min(1.0F, goldor.getHealth() / (float) ENTITY_HEALTH));
        BAR.setProgress(progress);
        long hp = Math.max(0L, Math.round(progress * MAX_HEALTH));
        BAR.setName(Component.literal("Goldor: " + String.format(java.util.Locale.ROOT, "%,d", hp)
                + " / 25,000,000 HP"));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == encounterLevel && player.distanceToSqr(goldor) <= 96.0D * 96.0D)
                BAR.addPlayer(player);
            else BAR.removePlayer(player);
        }
    }

    private static ServerPlayer nearestPlayer() {
        ServerPlayer nearest = null;
        double best = 64.0D * 64.0D;
        for (ServerPlayer player : encounterLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() != encounterLevel || !player.isAlive()) continue;
            double distance = player.distanceToSqr(goldor);
            if (distance < best) { best = distance; nearest = player; }
        }
        return nearest;
    }

    private static void renderWarning(BlockPos pos) {
        double x = pos.getX() + 0.5D, y = pos.getY(), z = pos.getZ() + 0.5D;
        encounterLevel.sendParticles(ParticleTypes.FLAME, x, y + 0.08D, z, 18, 0.35D, 0.02D, 0.35D, 0.01D);
        encounterLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.1D, z, 10, 0.35D, 0.02D, 0.35D, 0.02D);
    }

    private static void slam(BlockPos pos) {
        // Break exactly two layers. The third layer is the stopping layer and
        // is left untouched; bedrock is never removed.
        for (int depth = 1; depth <= 2; depth++) {
            BlockPos block = pos.below(depth);
            if (encounterLevel.getBlockState(block).is(Blocks.BEDROCK)) break;
            if (!encounterLevel.getBlockState(block).isAir()) {
                encounterLevel.destroyBlock(block, false);
            }
        }
        ACTIVE.add(new ActiveSword(pos, SWORD_LIFETIME));
        encounterLevel.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.65F);
        renderSword(pos);
    }

    /** Particle-built four-block sword silhouette; active hitbox lasts 30 seconds. */
    private static void renderSword(BlockPos pos) {
        double x = pos.getX() + 0.5D, z = pos.getZ() + 0.5D, y = pos.getY();
        for (int i = 0; i < 4; i++) {
            encounterLevel.sendParticles(ParticleTypes.CRIT, x, y + i + 0.5D, z,
                    5, 0.10D, 0.10D, 0.10D, 0.01D);
            encounterLevel.sendParticles(ParticleTypes.END_ROD, x, y + i + 0.5D, z,
                    2, 0.035D, 0.035D, 0.035D, 0.0D);
        }
        encounterLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 3.8D, z,
                4, 0.12D, 0.08D, 0.12D, 0.02D);
    }

    private static void finish() {
        if (encounterLevel == null) return;
        BAR.removeAllPlayers();
        PENDING.clear();
        ACTIVE.clear();
        encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Goldor has been defeated!"), false);
        clear();
    }

    private static void clear() {
        goldor = null;
        encounterLevel = null;
        testMode = false;
        attackTimer = 0;
        lastHealth = 0.0F;
        PENDING.clear();
        ACTIVE.clear();
    }

    private static final class PendingSword {
        private final BlockPos pos;
        private int ticks;
        private PendingSword(BlockPos pos, int ticks) { this.pos = pos; this.ticks = ticks; }
    }
    private static final class ActiveSword {
        private final BlockPos pos;
        private int ticks;
        private ActiveSword(BlockPos pos, int ticks) { this.pos = pos; this.ticks = ticks; }
    }
}
