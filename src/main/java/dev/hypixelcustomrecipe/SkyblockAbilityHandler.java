package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Java-driven item abilities migrated from the datapack. Each ability has its
 * own cooldown and spends from the player's shared mana pool.
 */
public final class SkyblockAbilityHandler {
    private SkyblockAbilityHandler() {
    }

    public static void initialize() {
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
            } else if (stack.is(DatapackItems.PET_ENDER_DRAGON)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.ENDER_DRAGON);
            } else if (stack.is(DatapackItems.PET_GOLDEN_DRAGON)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.GOLDEN_DRAGON);
            } else if (stack.is(DatapackItems.PET_IRON_GOLEM)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.IRON_GOLEM);
            } else if (stack.is(DatapackItems.PET_REMOVER)) {
                activatePet(serverPlayer, SkyblockPlayerState.Pet.NONE);
                serverPlayer.sendSystemMessage(Component.literal("Your pet has been removed. Your normal stats have been restored."));
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
        if (!beginAbility(player, "aote", 35, 4)) return;

        Vec3 destination = player.position().add(player.getLookAngle().scale(5.0));
        player.teleportTo(destination.x, destination.y, destination.z);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, 0.0, velocity.z);
        player.fallDistance = 0.0F;
        playSound(player, net.minecraft.sounds.SoundEvents.SHULKER_TELEPORT, 1.0F, 1.0F);
    }

    /** Aspect of the Void: raycast up to 64 blocks and land above the hit block. */
    private static void useAotv(ServerPlayer player) {
        if (!beginAbility(player, "aotv", 50, 60)) return;
        HitResult hit = player.pick(64.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        Vec3 destination = Vec3.atBottomCenterOf(blockHit.getBlockPos()).add(0.0, 1.0, 0.0);
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
        double damage = abilityDamage(player, 60.0);
        DamageSource source = level.damageSources().playerAttack(player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(5.0), entity -> isValidCombatTarget(entity, player))) {
            target.hurtServer(level, source, (float) damage);
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

    /** Aspect of the Dragon: a widening five-block breath dealing ability damage. */
    private static void useAotd(ServerPlayer player) {
        if (!beginAbility(player, "aotd", 80, 12)) return;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(direction.scale(0.3));
        Set<UUID> hitTargets = new HashSet<>();
        DamageSource source = level.damageSources().playerAttack(player);
        double damage = abilityDamage(player, 18.0);

        for (int step = 1; step <= 10; step++) {
            double distance = step * 0.5;
            double radius = 0.2 + step * 0.125;
            Vec3 center = origin.add(direction.scale(distance));
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                    18 + step * 2, radius, radius, radius, 0.01);
            AABB area = new AABB(center, center).inflate(radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> isValidTarget(entity, player))) {
                if (hitTargets.add(target.getUUID())) {
                    target.hurtServer(level, source, (float) damage);
                }
            }
        }
        playSound(player, net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 1.0F);
    }

    /** Necron's Scythe: a three-block forward slash, hitting each target once. */
    private static void useNecronsScythe(ServerPlayer player) {
        if (!beginAbility(player, "necrons_scythe", 35, 15)) return;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(0.0, -0.8, 0.0);
        Set<UUID> hitTargets = new HashSet<>();
        DamageSource source = level.damageSources().playerAttack(player);
        double damage = abilityDamage(player, 10.0);

        for (int step = 1; step <= 7; step++) {
            Vec3 center = origin.add(direction.scale(step * 0.5));
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                    5, 0.35, 0.25, 0.35, 0.01);
            AABB area = new AABB(center, center).inflate(1.5);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> isValidCombatTarget(entity, player))) {
                if (hitTargets.add(target.getUUID())) {
                    target.hurtServer(level, source, (float) damage);
                }
            }
        }
        playSound(player, net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
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
        player.sendSystemMessage(Component.literal("Damage: " + String.format(java.util.Locale.ROOT, "%.1f", stats.calculateWeaponDamage(false))));
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
        player.sendSystemMessage(Component.literal(
                "Searching within 32 blocks in this dimension..."));

        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = 32.0 * 32.0 + 1.0;

        for (int x = -32; x <= 32; x++) {
            for (int y = -32; y <= 32; y++) {
                for (int z = -32; z <= 32; z++) {
                    double distance = (double) x * x + (double) y * y + (double) z * z;
                    if (distance > 32.0 * 32.0 || distance >= nearestDistance) continue;

                    BlockPos candidate = origin.offset(x, y, z);
                    var block = level.getBlockState(candidate).getBlock();
                    if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                            || block == Blocks.ANCIENT_DEBRIS) {
                        nearest = candidate.immutable();
                        nearestDistance = distance;
                    }
                }
            }
        }

        if (nearest == null) {
            player.sendSystemMessage(Component.literal(
                    "No diamond ore or ancient debris found within 32 blocks in this dimension."));
        } else {
            player.sendSystemMessage(Component.literal(
                    "Nearest ore found at: X: " + nearest.getX()
                            + "  Y: " + nearest.getY() + "  Z: " + nearest.getZ()));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    nearest.getX() + 0.5, nearest.getY() + 0.8, nearest.getZ() + 0.5,
                    8, 0.25, 0.25, 0.25, 0.02);
        }
    }

    private static void activatePet(ServerPlayer player, SkyblockPlayerState.Pet pet) {
        SkyblockPlayerState.get(player).activatePet(player, pet);
        if (pet != SkyblockPlayerState.Pet.NONE) {
            player.sendSystemMessage(Component.literal(pet.name().replace('_', ' ') + " pet activated!"));
        }
    }


    /** Targets for breath attacks: players are excluded by the datapack selector. */
    private static boolean isValidTarget(LivingEntity entity, ServerPlayer caster) {
        return isValidCombatTarget(entity, caster) && !(entity instanceof Player);
    }

    /** Targets for implosion and scythe: other players can be hit too. */
    private static boolean isValidCombatTarget(LivingEntity entity, ServerPlayer caster) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return entity != caster && !type.equals("enderman")
                && !type.equals("armor_stand") && entity.isAlive();
    }

    private static double abilityDamage(ServerPlayer player, double baseDamage) {
        SkyblockPlayerState state = SkyblockPlayerState.get(player);
        SkyblockStats stats = state.stats();
        double base = baseDamage + (stats.strength() + stats.armorStrength()) * 0.1
                + stats.intelligence() * 0.2;
        return base * state.activePet().damageMultiplier();
    }

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
                                  float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound,
                net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch);
    }
}
