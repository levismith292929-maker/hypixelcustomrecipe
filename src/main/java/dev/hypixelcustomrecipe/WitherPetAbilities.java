package dev.hypixelcustomrecipe;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Special abilities shared by the four Wither boss pets. */
public final class WitherPetAbilities {
    private static final Map<UUID, Integer> GOLDOR_HITS = new ConcurrentHashMap<>();
    private static final List<SwordStrike> SWORD_STRIKES = new ArrayList<>();

    private WitherPetAbilities() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(WitherPetAbilities::tick);
    }

    public static boolean isWitherTrialEnemy(LivingEntity target) {
        return MaxorEncounter.isMaxor(target)
                || StormEncounter.isStorm(target)
                || GoldorEncounter.isGoldor(target)
                || NecronEncounter.isNecron(target)
                || NecronEncounter.isNecronMinion(target.getUUID());
    }

    /** Called only after a successful direct melee hit. */
    public static void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
        SkyblockPlayerState state = SkyblockPlayerState.get(attacker);
        if (state.activePet() != SkyblockPlayerState.Pet.GOLDOR
                || !attacker.getMainHandItem().is(DatapackItems.DARK_CLAYMORE)) return;

        int hits = GOLDOR_HITS.merge(attacker.getUUID(), 1, Integer::sum);
        if (hits >= 3) {
            GOLDOR_HITS.put(attacker.getUUID(), 0);
            summonSword(attacker, target);
        }
    }

    private static void summonSword(ServerPlayer attacker, LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive()) return;
        Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return;
        display.setItemStack(new ItemStack(Items.NETHERITE_SWORD));
        display.setItemTransform(ItemDisplayContext.FIXED);
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                new Vector3f(1.8F, 1.8F, 1.8F), new Quaternionf()));
        display.setPos(target.getX(), target.getY() + 2.5D, target.getZ());
        if (!level.addFreshEntity(display)) return;
        SWORD_STRIKES.add(new SwordStrike(level, target.getUUID(), attacker.getUUID(), display));
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (SkyblockPlayerState.get(player).activePet() == SkyblockPlayerState.Pet.NECRON
                    && player.tickCount % 20 == 0) {
                drainNearbyEnemies(player);
            }
        }

        Iterator<SwordStrike> iterator = SWORD_STRIKES.iterator();
        while (iterator.hasNext()) {
            SwordStrike strike = iterator.next();
            LivingEntity target = strike.level.getEntity(strike.targetId) instanceof LivingEntity living
                    ? living : null;
            if (target == null || !target.isAlive() || strike.display.isRemoved()) {
                strike.display.discard();
                iterator.remove();
                continue;
            }
            strike.age++;
            double progress = Math.min(1.0D, strike.age / 8.0D);
            double y = target.getY() + 2.5D - progress * 2.0D;
            strike.display.setPos(target.getX(), y, target.getZ());
            if (strike.age >= 8) {
                ServerPlayer attacker = server.getPlayerList().getPlayer(strike.attackerId);
                if (attacker != null && attacker.level() == strike.level) {
                    dealMaxHealthDamage(strike.level, attacker, target, 0.15D);
                }
                strike.level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.8D,
                        target.getZ(), 18, 0.35D, 0.35D, 0.35D, 0.15D);
                strike.level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 1.0F, 0.7F);
                strike.display.discard();
                iterator.remove();
            }
        }

        GOLDOR_HITS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static void drainNearbyEnemies(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        AABB bounds = owner.getBoundingBox().inflate(2.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != owner && !(entity instanceof Player)
                        && !(entity instanceof ArmorStand)
                        && !NecronGuardHandler.isNecronGuard(entity)
                        && entity.isAlive() && entity.distanceToSqr(owner) <= 4.0D)) {
            dealMaxHealthDamage(level, owner, target, 0.01D);
        }
    }

    private static void dealMaxHealthDamage(ServerLevel level, ServerPlayer attacker,
                                            LivingEntity target, double fraction) {
        if (target instanceof ServerPlayer defender) {
            SkyblockPlayerState state = SkyblockPlayerState.get(defender);
            float damage = (float) (state.maximumHealth() * fraction);
            state.markCombat();
            boolean defeated = state.applyTrueDamage(damage);
            DamageNumberHandler.show(level, defender, damage, false);
            if (defeated) {
                defender.setHealth(0.0F);
                defender.die(level.damageSources().magic());
            }
            return;
        }

        double maximumHealth = target.getMaxHealth();
        double requestedDamage = maximumHealth * fraction;
        double actualDamage = Math.min(target.getHealth(), requestedDamage);
        if (actualDamage <= 0.0D) return;

        if (MaxorEncounter.isMaxor(target)) {
            MaxorEncounter.recordDirectDamage(target, attacker, actualDamage);
            target.setHealth((float) Math.max(0.0D, target.getHealth() - requestedDamage));
        } else if (StormEncounter.isStorm(target)) {
            StormEncounter.recordDirectDamage(target, attacker, actualDamage);
            target.setHealth((float) Math.max(0.0D, target.getHealth() - requestedDamage));
        } else if (GoldorEncounter.isGoldor(target)) {
            // Goldor's direct-damage method also applies the health subtraction.
            GoldorEncounter.recordDirectDamage(target, attacker, actualDamage);
        } else if (NecronEncounter.isNecron(target)) {
            NecronEncounter.recordDirectDamage(target, attacker, actualDamage);
            target.setHealth((float) Math.max(0.0D, target.getHealth() - requestedDamage));
        } else {
            target.setHealth((float) Math.max(0.0D, target.getHealth() - requestedDamage));
        }
        if (target.getHealth() <= 0.0F && target.isAlive()) target.die(level.damageSources().magic());
        DamageNumberHandler.show(level, target, (float) actualDamage, false);
    }

    private static final class SwordStrike {
        private final ServerLevel level;
        private final UUID targetId;
        private final UUID attackerId;
        private final Display.ItemDisplay display;
        private int age;

        private SwordStrike(ServerLevel level, UUID targetId, UUID attackerId,
                            Display.ItemDisplay display) {
            this.level = level;
            this.targetId = targetId;
            this.attackerId = attackerId;
            this.display = display;
        }
    }
}
