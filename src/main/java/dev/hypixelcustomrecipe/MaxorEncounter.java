package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
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
    private static final double MAXOR_HEALTH = 2_000_000.0D;
    private static final double REWARD_THRESHOLD = MAXOR_HEALTH * 0.20D;
    private static final int MINION_INTERVAL_TICKS = 80;
    private static final int MINION_BATCH_SIZE = 8;
    private static final int MAX_LIVING_MINIONS = 48;

    private static WitherSkeleton maxor;
    private static ServerLevel encounterLevel;
    private static UUID altarOwner;
    private static int minionSpawnTimer;
    private static final Map<UUID, Double> PLAYER_DAMAGE = new HashMap<>();
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final List<UUID> MINIONS = new ArrayList<>();
    private static final ServerBossEvent BOSS_BAR = new ServerBossEvent(
            UUID.randomUUID(),
            Component.literal("Maxor"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private MaxorEncounter() {
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
            start(serverPlayer, serverLevel);
            if (!serverPlayer.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (maxor == null || entity != maxor || amount <= 0.0F) {
                return true;
            }
            Entity attacker = source.getEntity();
            if (attacker instanceof ServerPlayer player) {
                PLAYER_DAMAGE.merge(player.getUUID(), (double) amount, Double::sum);
                PARTICIPANTS.add(player.getUUID());
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (maxor != null && entity.getUUID().equals(maxor.getUUID())) {
                finishSuccessfully();
                return;
            }
            if (entity instanceof ServerPlayer player && PARTICIPANTS.contains(player.getUUID())) {
                abortBecausePlayerDied(player);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(MaxorEncounter::tick);
    }

    private static void start(ServerPlayer starter, ServerLevel level) {
        WitherSkeleton boss = EntityType.WITHER_SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) {
            starter.sendSystemMessage(Component.literal("Maxor could not be summoned here.")
                    .withStyle(style -> style.withColor(0xFF5555)));
            return;
        }

        BlockPos spawnPos = starter.blockPosition().relative(starter.getDirection(), 5);
        boss.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        boss.setCustomName(Component.literal("Maxor").withStyle(style -> style
                .withColor(0xAA00AA).withBold(true)));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        var maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(MAXOR_HEALTH);
        }
        boss.setHealth((float) MAXOR_HEALTH);
        boss.setTarget(starter);

        if (!level.addFreshEntity(boss)) {
            starter.sendSystemMessage(Component.literal("Maxor could not be spawned here.")
                    .withStyle(style -> style.withColor(0xFF5555)));
            return;
        }

        maxor = boss;
        encounterLevel = level;
        altarOwner = starter.getUUID();
        minionSpawnTimer = MINION_INTERVAL_TICKS;
        PLAYER_DAMAGE.clear();
        PARTICIPANTS.clear();
        MINIONS.clear();
        PARTICIPANTS.add(starter.getUUID());
        BOSS_BAR.setName(Component.literal("Maxor"));
        BOSS_BAR.setProgress(1.0F);
        BOSS_BAR.addPlayer(starter);
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.0F, 0.7F);
        starter.sendSystemMessage(Component.literal("MAXOR HAS AWAKENED!")
                .withStyle(style -> style.withColor(0xAA00AA).withBold(true)));
    }

    private static void tick(MinecraftServer server) {
        if (maxor == null || encounterLevel == null) {
            return;
        }
        if (!maxor.isAlive() || maxor.isRemoved()) {
            return;
        }

        BOSS_BAR.setProgress(Math.max(0.0F, Math.min(1.0F, maxor.getHealth() / (float) MAXOR_HEALTH)));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel() == encounterLevel
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
            WitherSkeleton skeleton = EntityType.WITHER_SKELETON.create(
                    encounterLevel, EntitySpawnReason.MOB_SUMMONED);
            if (skeleton == null) {
                continue;
            }
            double angle = encounterLevel.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 3.0D + encounterLevel.random.nextDouble() * 4.0D;
            skeleton.setPos(
                    maxor.getX() + Math.cos(angle) * distance,
                    maxor.getY(),
                    maxor.getZ() + Math.sin(angle) * distance
            );
            skeleton.setPersistenceRequired();
            ServerPlayer target = nearestParticipant();
            if (target != null) {
                skeleton.setTarget(target);
            }
            if (encounterLevel.addFreshEntity(skeleton)) {
                MINIONS.add(skeleton.getUUID());
            }
        }
    }

    private static ServerPlayer nearestParticipant() {
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (UUID uuid : PARTICIPANTS) {
            ServerPlayer player = encounterLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || player.serverLevel() != encounterLevel || !player.isAlive()) {
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
        int rewarded = 0;
        for (Map.Entry<UUID, Double> entry : PLAYER_DAMAGE.entrySet()) {
            if (entry.getValue() < REWARD_THRESHOLD) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                // 50 SkyBlock XP per level: 5,000 XP grants exactly 100 levels.
                SkyblockPlayerState.get(player).addLevelExperience(player, 5_000);
                player.sendSystemMessage(Component.literal("Maxor defeated! +100 Player Levels")
                        .withStyle(style -> style.withColor(0x55FF55).withBold(true)));
                rewarded++;
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
                Component.literal("Maxor has been defeated! " + rewarded + " player(s) earned the reward."),
                false);
        clearEncounter();
    }

    private static void abortBecausePlayerDied(ServerPlayer deadPlayer) {
        if (maxor == null) {
            return;
        }
        MinecraftServer server = encounterLevel.getServer();
        for (UUID uuid : MINIONS) {
            Entity entity = encounterLevel.getEntity(uuid);
            if (entity != null) {
                entity.discard();
            }
        }
        maxor.discard();
        BOSS_BAR.removeAllPlayers();

        ServerPlayer owner = server.getPlayerList().getPlayer(altarOwner);
        if (owner != null) {
            ItemStack altar = new ItemStack(ModItems.WITHER_ALTAR);
            if (!owner.getInventory().add(altar)) {
                owner.drop(altar, false);
            }
            owner.sendSystemMessage(Component.literal("The encounter failed. Your Wither Altar has been returned.")
                    .withStyle(style -> style.withColor(0xFF5555)));
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(deadPlayer.getName().getString()
                        + " died. The Maxor encounter has ended."),
                false);
        clearEncounter();
    }

    private static void clearEncounter() {
        maxor = null;
        encounterLevel = null;
        altarOwner = null;
        minionSpawnTimer = 0;
        PLAYER_DAMAGE.clear();
        PARTICIPANTS.clear();
        MINIONS.clear();
    }
}
