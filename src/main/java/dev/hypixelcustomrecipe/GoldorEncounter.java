package dev.hypixelcustomrecipe;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Goldor's chase-and-sword-slam encounter. */
public final class GoldorEncounter {
    private static final double MAX_HEALTH = 25_000_000.0D;
    private static final double ENTITY_HEALTH = 1_024.0D;
    private static final int WARNING_TICKS = 40;
    private static final int SWORD_LIFETIME = 600;
    private static final int FLIGHT_TICKS = 4;
    private static final int ATTACK_INTERVAL = 60;
    private static final double SWORD_RADIUS = 1.5D;

    private static WitherBoss goldor;
    private static ServerLevel encounterLevel;
    private static int attackTimer;
    private static float lastHealth;
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final Set<UUID> DEAD_PARTICIPANTS = new HashSet<>();
    private static final List<PendingSword> PENDING = new ArrayList<>();
    private static final List<FlyingSword> FLYING = new ArrayList<>();
    private static final List<ActiveSword> ACTIVE = new ArrayList<>();
    private static final ServerBossEvent BAR = new ServerBossEvent(
            UUID.randomUUID(), Component.literal("Goldor"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    private GoldorEncounter() {}

    public static boolean isActive() { return goldor != null && goldor.isAlive(); }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (goldor != null && entity == goldor
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && PARTICIPANTS.contains(player.getUUID())) {
                DEAD_PARTICIPANTS.add(player.getUUID());
                if (encounterLevel != null && allFightersDead(player.level().getServer())) {
                    abortBecauseAllFightersDied();
                    return;
                }
            }
            if (goldor != null && entity == goldor) {
                if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    abortAfterBossRemoval();
                } else {
                    finish();
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(GoldorEncounter::tick);
    }

    public static boolean isGoldor(net.minecraft.world.entity.LivingEntity entity) {
        return goldor != null && entity == goldor;
    }

    public static void recordDirectDamage(net.minecraft.world.entity.LivingEntity target,
                                          ServerPlayer attacker, double entityDamage) {
        if (goldor == null || target != goldor || entityDamage <= 0.0D) return;
        PARTICIPANTS.add(attacker.getUUID());
        float next = (float) Math.max(0.0D, goldor.getHealth() - entityDamage);
        goldor.setHealth(next);
        lastHealth = next;
        if (next <= 0.0F) goldor.die(encounterLevel.damageSources().generic());
    }

    public static boolean summonForTesting(ServerPlayer player) {
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
        attackTimer = 60;
        lastHealth = (float) ENTITY_HEALTH;
        clearSwords();
        PARTICIPANTS.clear();
        DEAD_PARTICIPANTS.clear();
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
        if (allFightersDead(server)) {
            abortBecauseAllFightersDied();
            return;
        }

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
            Vec3 lockedPosition = target.position();
            List<BlockPos> randomTargets = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
                double radius = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * 4.0D;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                randomTargets.add(BlockPos.containing(lockedPosition.x + offsetX,
                        lockedPosition.y, lockedPosition.z + offsetZ));
            }
            PENDING.add(new PendingSword(randomTargets, WARNING_TICKS, createWarningDisplays(randomTargets)));
            attackTimer = ATTACK_INTERVAL;
            target.sendSystemMessage(Component.literal("GOLDOR IS TARGETING YOU!")
                    .withStyle(s -> s.withColor(0xFF5555).withBold(true)));
        }

        Iterator<PendingSword> pending = PENDING.iterator();
        while (pending.hasNext()) {
            PendingSword sword = pending.next();
            if (--sword.ticks <= 0) {
                for (Display.BlockDisplay warning : sword.warnings) warning.discard();
                for (BlockPos destination : sword.positions) FLYING.add(createFlyingSword(destination));
                pending.remove();
            }
        }

        Iterator<FlyingSword> flying = FLYING.iterator();
        while (flying.hasNext()) {
            FlyingSword sword = flying.next();
            sword.ticks++;
            double progress = Math.min(1.0D, sword.ticks / (double) FLIGHT_TICKS);
            Vec3 position = sword.start.lerp(sword.destination, progress);
            sword.display.setPos(position.x, position.y, position.z);
            pointSwordToward(sword.display, sword.destination.subtract(position));
            if (progress >= 1.0D) {
                slam(sword.pos, sword.display);
                flying.remove();
            }
        }

        Iterator<ActiveSword> active = ACTIVE.iterator();
        while (active.hasNext()) {
            ActiveSword sword = active.next();
            if (--sword.ticks <= 0 || sword.display.isRemoved()) {
                sword.display.discard();
                active.remove();
                continue;
            }
            AABB hitbox = new AABB(sword.pos.getX() + 0.5D - SWORD_RADIUS, sword.pos.getY(),
                    sword.pos.getZ() + 0.5D - SWORD_RADIUS,
                    sword.pos.getX() + 0.5D + SWORD_RADIUS, sword.pos.getY() + 4.0D,
                    sword.pos.getZ() + 0.5D + SWORD_RADIUS);
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

    private static List<Display.BlockDisplay> createWarningDisplays(List<BlockPos> positions) {
        List<Display.BlockDisplay> displays = new ArrayList<>();
        for (BlockPos pos : positions) {
            Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(encounterLevel, EntitySpawnReason.COMMAND);
            if (display == null) continue;
            display.setBlockState(Blocks.ORANGE_WOOL.defaultBlockState());
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                    new Vector3f(1.5F, 0.08F, 1.5F), new Quaternionf()));
            display.setNoGravity(true);
            display.setPos(pos.getX() + 0.5D, pos.getY() + 0.08D, pos.getZ() + 0.5D);
            if (encounterLevel.addFreshEntity(display)) displays.add(display);
        }
        return displays;
    }

    private static FlyingSword createFlyingSword(BlockPos pos) {
        Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(encounterLevel, EntitySpawnReason.COMMAND);
        if (display == null) throw new IllegalStateException("Could not create Goldor's sword display");
        display.setItemStack(new ItemStack(Items.NETHERITE_SWORD));
        display.setItemTransform(ItemDisplayContext.FIXED);
        display.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F),
                new Quaternionf(), new Vector3f(4.0F, 4.0F, 4.0F), new Quaternionf()));
        Vec3 start = goldor.position().add(0.0D, 2.0D, 0.0D);
        Vec3 destination = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        display.setPos(start.x, start.y, start.z);
        pointSwordToward(display, destination.subtract(start));
        if (!encounterLevel.addFreshEntity(display)) throw new IllegalStateException("Could not spawn Goldor's sword");
        encounterLevel.playSound(null, goldor.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
        return new FlyingSword(pos, display, start, destination);
    }

    private static void pointSwordToward(Display.ItemDisplay display, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-8D) return;
        Vec3 normalized = direction.normalize();
        // A sword's blade runs along the item's local +Y axis. Rotate the item itself
        // so the blade tip, rather than only the display entity, faces the destination.
        Quaternionf bladeRotation = new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F),
                new Vector3f((float) normalized.x, (float) normalized.y, (float) normalized.z));
        display.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F),
                bladeRotation, new Vector3f(4.0F, 4.0F, 4.0F), new Quaternionf()));
    }

    private static void slam(BlockPos pos, Display.ItemDisplay display) {
        // The impact footprint is a 3-block diameter (1.5-block radius).
        // Each column loses the first two layers; its third layer is untouched.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx * dx + dz * dz > SWORD_RADIUS * SWORD_RADIUS) continue;
                for (int depth = 1; depth <= 2; depth++) {
                    BlockPos block = pos.offset(dx, -depth, dz);
                    if (encounterLevel.getBlockState(block).is(Blocks.BEDROCK)) break;
                    if (!encounterLevel.getBlockState(block).isAir()) encounterLevel.destroyBlock(block, false);
                }
            }
        }
        ACTIVE.add(new ActiveSword(pos, display, SWORD_LIFETIME));
        encounterLevel.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.65F);
    }

    private static void finish() {
        if (encounterLevel == null) return;
        BAR.removeAllPlayers();
        clearSwords();
        encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Goldor has been defeated!"), false);
        clear();
    }

    private static void abortAfterBossRemoval() {
        if (encounterLevel == null) return;
        BAR.removeAllPlayers();
        clearSwords();
        encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Goldor was removed. The encounter has been reset."), false);
        clear();
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
        if (encounterLevel == null) return;
        if (goldor != null) goldor.discard();
        BAR.removeAllPlayers();
        clearSwords();
        encounterLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("All players fighting Goldor have been defeated. The encounter has ended."), false);
        clear();
    }

    private static void clearSwords() {
        if (encounterLevel != null) {
            for (PendingSword sword : PENDING) for (Display.BlockDisplay warning : sword.warnings) warning.discard();
            for (FlyingSword sword : FLYING) sword.display.discard();
            for (ActiveSword sword : ACTIVE) sword.display.discard();
        }
        PENDING.clear();
        FLYING.clear();
        ACTIVE.clear();
    }

    private static void clear() {
        goldor = null;
        encounterLevel = null;
        attackTimer = 0;
        lastHealth = 0.0F;
        PENDING.clear();
        FLYING.clear();
        ACTIVE.clear();
        PARTICIPANTS.clear();
    }

    private static final class PendingSword {
        private final List<BlockPos> positions;
        private final List<Display.BlockDisplay> warnings;
        private int ticks;
        private PendingSword(List<BlockPos> positions, int ticks, List<Display.BlockDisplay> warnings) {
            this.positions = positions;
            this.ticks = ticks;
            this.warnings = warnings;
        }
    }

    private static final class FlyingSword {
        private final BlockPos pos;
        private final Display.ItemDisplay display;
        private final Vec3 start;
        private final Vec3 destination;
        private int ticks;
        private FlyingSword(BlockPos pos, Display.ItemDisplay display, Vec3 start, Vec3 destination) {
            this.pos = pos; this.display = display; this.start = start; this.destination = destination;
        }
    }

    private static final class ActiveSword {
        private final BlockPos pos;
        private final Display.ItemDisplay display;
        private int ticks;
        private ActiveSword(BlockPos pos, Display.ItemDisplay display, int ticks) {
            this.pos = pos; this.display = display; this.ticks = ticks;
        }
    }
}
