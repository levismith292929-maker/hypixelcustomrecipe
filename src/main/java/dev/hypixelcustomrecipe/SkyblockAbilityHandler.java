package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java-driven item abilities migrated from the datapack. Each ability has its
 * own cooldown and spends from the player's shared mana pool.
 */
public final class SkyblockAbilityHandler {
    private static final Map<UUID, BreathState> ACTIVE_BREATHS = new ConcurrentHashMap<>();
    private static final Map<UUID, SlashState> ACTIVE_SLASHES = new ConcurrentHashMap<>();

    private SkyblockAbilityHandler() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BreathState breath = ACTIVE_BREATHS.get(player.getUUID());
                if (breath != null) tickDragonBreath(player, breath);
                SlashState slash = ACTIVE_SLASHES.get(player.getUUID());
                if (slash != null) tickScytheSlash(player, slash);
                OreTrackerSearch.tick(player);
            }
            ACTIVE_BREATHS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
            ACTIVE_SLASHES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ACTIVE_BREATHS.clear();
            ACTIVE_SLASHES.clear();
            OreTrackerSearch.clear();
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (stack.is(ModItems.AOTE)) {
                useAote(serverPlayer);
            } else if (stack.is(DatapackItems.AOTV)) {
                useAotv(serverPlayer);
            } else if (stack.is(DatapackItems.HYPERION)) {
                useHyperion(serverPlayer);
            } else if (stack.is(DatapackItems.ASTRAEA)) {
                useAstraea(serverPlayer);
            } else if (stack.is(DatapackItems.AOTD)) {
                useAotd(serverPlayer);
            } else if (stack.is(DatapackItems.NECRONS_SCYTHE)) {
                useNecronsScythe(serverPlayer);
            } else if (stack.is(DatapackItems.ZOMBIE_SWORD)) {
                useZombieSword(serverPlayer);
            } else if (stack.is(DatapackItems.ORE_TRACKER)) {
                useOreTracker(serverPlayer);
            } else if (stack.is(DatapackItems.STAT_VIEWER)) {
                showStats(serverPlayer);
            } else if (stack.is(DatapackItems.PET_ENDERMAN)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.ENDERMAN);
                if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            } else if (stack.is(DatapackItems.PET_ENDER_DRAGON)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.ENDER_DRAGON);
                if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            } else if (stack.is(DatapackItems.PET_GOLDEN_DRAGON)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.GOLDEN_DRAGON);
                if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            } else if (stack.is(DatapackItems.PET_IRON_GOLEM)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.IRON_GOLEM);
                if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            } else if (stack.is(DatapackItems.PET_REMOVER)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.NONE);
                serverPlayer.sendSystemMessage(Component.literal("Your pet has been removed. Your normal stats have been restored."));
                if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            } else {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static SkyblockManaState mana(ServerPlayer player) {
        return SkyblockPlayerState.get(player).mana();
    }

    private static boolean beginAbility(ServerPlayer player, String id, int cost, int cooldown) {
        SkyblockManaState mana = mana(player);
        if (mana.isOnCooldown(id)) return false;
        if (!mana.spend(cost)) {
            player.sendSystemMessage(Component.literal("Not enough mana! (" + cost + " required)"));
            return false;
        }
        mana.setRegenerationDelay(40);
        mana.startCooldown(id, cooldown);
        return true;
    }

    private static void useAote(ServerPlayer player) {
        if (!beginAbility(player, "aote", 15, 2)) return;

        Vec3 destination = player.position().add(player.getLookAngle().scale(5.0));
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        playSound(player, net.minecraft.sounds.SoundEvents.SHULKER_TELEPORT, 1.0F, 1.0F);
    }


    /** Shared preview entry point so the HUD uses the exact ability raycast. */
    public static BlockPos findRaycastTargetForPreview(ServerPlayer player) {
        return findRaycastTarget(player);
    }

    /** Reproduces the datapack's 640-step, 0.1-block raycast. */
    private static BlockPos findRaycastTarget(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        for (int step = 0; step < 640; step++) {
            Vec3 sample = eye.add(direction.scale(step * 0.1));
            BlockPos pos = BlockPos.containing(sample);
            if (!isRaycastIgnored(level.getBlockState(pos))) return pos;
        }
        return null;
    }

    private static boolean isRaycastIgnored(net.minecraft.world.level.block.state.BlockState state) {
        return state.isAir()
                || state.is(BlockTags.REPLACEABLE)
                || state.getFluidState().is(FluidTags.WATER)
                || state.getFluidState().is(FluidTags.LAVA)
                || state.is(Blocks.BUBBLE_COLUMN)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LIGHT);
    }

    /** Aspect of the Void: raycast up to 64 blocks and land above the hit block. */
    private static void useAotv(ServerPlayer player) {
        if (!beginAbility(player, "aotv", 50, 3)) return;
        BlockPos target = findRaycastTarget(player);
        if (target == null) return;
        Vec3 destination = Vec3.atBottomCenterOf(target).add(0.0, 1.0, 0.0);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0, destination.z,
                45, 0.45, 0.8, 0.45, 0.15);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, destination.x, destination.y + 1.0, destination.z,
                25, 0.3, 0.6, 0.3, 0.05);
        playSound(player, net.minecraft.sounds.SoundEvents.SHULKER_TELEPORT, 1.0F, 1.0F);
    }

    /** Hyperion: 60 ability damage to living non-player targets within five blocks. */
    private static void useHyperion(ServerPlayer player) {
        if (!beginAbility(player, "hyperion", 150, 100)) return;
        ServerLevel level = (ServerLevel) player.level();
        double baseDamage = 60.0;
        DamageSource source = level.damageSources().explosion(null, player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(5.0), entity -> isValidCombatTarget(entity, player)
                        && entity.distanceToSqr(player) <= 25.0)) {
            target.hurtServer(level, source, (float) abilityDamage(player, target, baseDamage));
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(),
                30, 1.5, 0.6, 1.5, 0.05);
        playSound(player, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);
    }

    /** Astraea: Resistance II and Absorption IV for ten seconds. */
    private static void useAstraea(ServerPlayer player) {
        if (!beginAbility(player, "astraea", 40, 200)) return;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3));
        SkyblockPlayerState.get(player).startAstraeaOrbs();
        playSound(player, net.minecraft.sounds.SoundEvents.WITHER_AMBIENT, 0.6F, 1.6F);
    }

    /** Starts the datapack's ten-tick, widening dragon-breath beam. */
    private static void useAotd(ServerPlayer player) {
        if (!beginAbility(player, "aotd", 80, 12)) return;
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(direction.scale(0.3)).add(0.0, -0.1, 0.0);
        double baseDamage = 18.0;
        ACTIVE_BREATHS.put(player.getUUID(), new BreathState(origin, direction, baseDamage));
        playSound(player, net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 1.0F);
    }

    private static void tickDragonBreath(ServerPlayer player, BreathState breath) {
        ServerLevel level = (ServerLevel) player.level();
        int step = ++breath.step;
        double distance = step * 0.5;
        double radius = 0.2 + step * 0.125;
        Vec3 center = breath.origin.add(breath.direction.scale(distance));

        Vec3 right = breath.direction.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1.0, 0.0, 0.0);
        right = right.normalize();
        Vec3 up = right.cross(breath.direction).normalize();

        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0 * i) / 24.0;
            Vec3 point = center.add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        AABB area = new AABB(center, center).inflate(radius + 0.295);
        DamageSource source = level.damageSources().source(DamageTypes.DRAGON_BREATH, null, player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> isValidTarget(entity, player))) {
            if (breath.hitTargets.add(target.getUUID())) {
                target.hurtServer(level, source, (float) abilityDamage(player, target, breath.baseDamage));
            }
        }

        if (step >= 10) ACTIVE_BREATHS.remove(player.getUUID());
    }

    private static final class BreathState {
        private final Vec3 origin;
        private final Vec3 direction;
        private final double baseDamage;
        private final Set<UUID> hitTargets = new HashSet<>();
        private int step;

        private BreathState(Vec3 origin, Vec3 direction, double baseDamage) {
            this.origin = origin;
            this.direction = direction;
            this.baseDamage = baseDamage;
        }
    }

    /** Necron's Scythe: seven close-range hit checks and a five-tick slash visual. */
    private static void useNecronsScythe(ServerPlayer player) {
        if (!beginAbility(player, "necrons_scythe", 150, 4)) return;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(0.0, -0.8, 0.0);
        Set<UUID> hitTargets = new HashSet<>();
        DamageSource source = level.damageSources().source(DamageTypes.DRAGON_BREATH, null, player);
        double baseDamage = 1350.0;
        double[] scanDistances = {0.25, 0.75, 1.25, 1.75, 2.25, 2.75, 3.0};

        for (double distance : scanDistances) {
            Vec3 center = origin.add(direction.scale(distance));
            AABB area = new AABB(center, center).inflate(1.5);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> isValidScytheTarget(entity, player)
                            && entity.distanceToSqr(center) <= 2.25)) {
                if (hitTargets.add(target.getUUID())) {
                    target.hurtServer(level, source, (float) scytheAbilityDamage(player, target, baseDamage));
                }
            }
        }

        Vec3 slashCenter = player.getEyePosition().add(direction.scale(3.0));
        Vec3 right = direction.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1.0, 0.0, 0.0);
        ACTIVE_SLASHES.put(player.getUUID(), new SlashState(slashCenter, right.normalize()));
        playSound(player, net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
    }

    private static void tickScytheSlash(ServerPlayer player, SlashState slash) {
        int tick = ++slash.age;
        if (tick == 1 || tick == 2 || tick == 3) {
            double first = -0.9 + (tick - 1) * 0.6;
            double second = first + 0.3;
            spawnSlashParticle(player, slash, first);
            spawnSlashParticle(player, slash, second);
        } else if (tick == 4) {
            spawnSlashParticle(player, slash, 0.75);
        } else if (tick == 5) {
            spawnSlashParticle(player, slash, 0.9);
        } else {
            ACTIVE_SLASHES.remove(player.getUUID());
        }
    }

    private static void spawnSlashParticle(ServerPlayer player, SlashState slash, double offset) {
        Vec3 point = slash.center.add(slash.right.scale(offset));
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.END_ROD,
                point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static final class SlashState {
        private final Vec3 center;
        private final Vec3 right;
        private int age;

        private SlashState(Vec3 center, Vec3 right) {
            this.center = center;
            this.right = right;
        }
    }

    /** Zombie Sword: restore one heart, with its one-second cooldown. */
    private static void useZombieSword(ServerPlayer player) {
        if (!beginAbility(player, "zombie_sword", 50, 20)) return;
        SkyblockPlayerState.get(player).heal(20);
        playSound(player, net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE, 0.7F, 1.4F);
    }



    /** Displays the same core stat categories exposed by the datapack viewer. */
    private static void showStats(ServerPlayer player) {
        SkyblockPlayerState state = SkyblockPlayerState.get(player);
        SkyblockStats stats = state.stats();
        SkyblockManaState mana = state.mana();
        player.sendSystemMessage(Component.literal("Your Stats").withStyle(style -> style.withColor(0xFFAA00).withBold(true)));
        player.sendSystemMessage(Component.literal("SkyBlock Level: " + state.level()
                + " (" + state.levelExperience() + "/" + state.levelExperienceRequired() + " XP)"));
        player.sendSystemMessage(Component.literal("Damage: " + String.format(java.util.Locale.ROOT, "%.0f", stats.calculateWeaponDamage(false))));
        player.sendSystemMessage(Component.literal("Strength: " + stats.strength()));
        player.sendSystemMessage(Component.literal("Armor Strength: " + stats.armorStrength()));
        player.sendSystemMessage(Component.literal("Crit Chance: " + stats.critChance() + "%"));
        player.sendSystemMessage(Component.literal("Crit Damage: " + stats.critDamage() + "%"));
        player.sendSystemMessage(Component.literal("Maximum Mana: " + mana.maximumMana()));
        player.sendSystemMessage(Component.literal("Mana Regeneration: " + mana.regenerationPerSecond() + " / second"));
        player.sendSystemMessage(Component.literal("Defense: " + stats.defense()));
        player.sendSystemMessage(Component.literal("Intelligence: " + stats.intelligence()));
    }

    /** Searches the current dimension for the nearest diamond ore or ancient debris. */
    private static void useOreTracker(ServerPlayer player) {
        if (!beginAbility(player, "ore_tracker", 100, 600)) return;
        OreTrackerSearch.start(player);
    }

    private static void activatePet(ServerPlayer player, SkyblockPlayerState.Pet pet) {
        SkyblockPlayerState.get(player).activatePet(player, pet);
        if (pet != SkyblockPlayerState.Pet.NONE) {
            player.sendSystemMessage(Component.literal(pet.name().replace('_', ' ') + " pet activated!"));
        }
    }


    /** Dragon breath excludes players, but otherwise hits all living entities. */
    private static boolean isValidTarget(LivingEntity entity, ServerPlayer caster) {
        return entity != caster && !(entity instanceof Player) && entity.isAlive();
    }

    /** The scythe's original selector can hit players, Endermen, and armor stands. */
    private static boolean isValidScytheTarget(LivingEntity entity, ServerPlayer caster) {
        return entity != caster && entity.isAlive();
    }

    /** Targets for implosion and scythe: other players can be hit too. */
    private static boolean isValidCombatTarget(LivingEntity entity, ServerPlayer caster) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return entity != caster && !type.equals("enderman")
                && !type.equals("armor_stand") && entity.isAlive();
    }

    /** Necron's Scythe loses 3% damage for each digit in the target's maximum health. */
    private static double scytheAbilityDamage(ServerPlayer player, LivingEntity target, double baseDamage) {
        double damage = abilityDamage(player, target, baseDamage);
        double maxHealth = target.getMaxHealth();
        int digits = maxHealth >= 1.0
                ? (int) Math.floor(Math.log10(maxHealth)) + 1
                : 1;
        double reduction = Math.min(1.0, digits * 0.03);
        return damage * (1.0 - reduction);
    }

    private static double abilityDamage(ServerPlayer player, LivingEntity target, double baseDamage) {
        SkyblockPlayerState state = SkyblockPlayerState.get(player);
        SkyblockStats stats = state.stats();
        double base = baseDamage + (stats.strength() + stats.armorStrength()) * 0.1
                + stats.intelligence() * 0.2;
        double damage = Math.round(base * state.activePet().damageMultiplier());

        int chance = Math.min(100, stats.critChance());
        if (chance > 0 && player.getRandom().nextInt(100) < chance) {
            damage *= stats.criticalMultiplier();
        }
        return damage;
    }

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
                                  float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound,
                net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch);
    }
}
