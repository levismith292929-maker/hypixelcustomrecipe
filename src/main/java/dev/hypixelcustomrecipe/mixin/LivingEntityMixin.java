package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.AccessoryBag;
import dev.hypixelcustomrecipe.DamageNumberHandler;
import dev.hypixelcustomrecipe.DatapackItems;
import dev.hypixelcustomrecipe.ModArmorItems;
import dev.hypixelcustomrecipe.SkyblockPlayerState;
import dev.hypixelcustomrecipe.SkyblockStats;
import dev.hypixelcustomrecipe.SkyblockLevelBonuses;
import dev.hypixelcustomrecipe.VanillaWeaponStats;
import dev.hypixelcustomrecipe.VengenceHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private static final ThreadLocal<Boolean> hypixelcustomrecipe$reapplyingDamage =
            ThreadLocal.withInitial(() -> false);

    @Unique
    private float hypixelcustomrecipe$healthBeforeHit;

    @Unique
    private boolean hypixelcustomrecipe$trackingPlayerDamage;

    @Unique
    private boolean hypixelcustomrecipe$trackingDamageNumber;

    @Unique
    private boolean hypixelcustomrecipe$combatXpAwarded;

    @Unique
    private boolean hypixelcustomrecipe$criticalHit;

    @Unique
    private float hypixelcustomrecipe$damageNumberAmount;

    @Inject(method = "tick", at = @At("HEAD"))
    private void hypixelcustomrecipe$voidriddenEscapeBoats(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenZombie
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenSkeleton
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEnderman
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenSpider
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenCreeper)) {
            return;
        }

        net.minecraft.world.entity.Entity vehicle = entity.getVehicle();
        if (vehicle == null) return;
        String vehicleId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(vehicle.getType()).getPath();
        if (vehicleId.endsWith("_boat") || vehicleId.endsWith("_chest_boat")
                || vehicleId.endsWith("_raft")) {
            entity.stopRiding();
        }
    }

    @Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
    private void hypixelcustomrecipe$useVoidriddenMaxHealth(
            CallbackInfoReturnable<Float> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenZombie
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenSkeleton
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenCreeper) {
            cir.setReturnValue(100000.0F);
        } else if (entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEnderman) {
            cir.setReturnValue(200000.0F);
        } else if (entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenSpider) {
            cir.setReturnValue(80000.0F);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$applySkyblockAttack(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (hypixelcustomrecipe$reapplyingDamage.get()) return;

        LivingEntity target = (LivingEntity) (Object) this;
        hypixelcustomrecipe$healthBeforeHit = target.getHealth();
        hypixelcustomrecipe$trackingPlayerDamage = target instanceof ServerPlayer;
        hypixelcustomrecipe$trackingDamageNumber =
                source.getEntity() instanceof ServerPlayer attacker && attacker != target
                        && !(target instanceof ServerPlayer);
        hypixelcustomrecipe$criticalHit = false;
        hypixelcustomrecipe$damageNumberAmount = amount;

        float adjusted = amount;
        boolean soulBladeAttack = source.getEntity() instanceof ServerPlayer soulBladeAttacker
                && soulBladeAttacker != target && source.getDirectEntity() == soulBladeAttacker
                && soulBladeAttacker.getMainHandItem().is(DatapackItems.SOUL_BLADE);

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target
                && source.getDirectEntity() == attacker) {
            if (soulBladeAttack) {
                adjusted = target instanceof ServerPlayer defenderTarget
                        ? SkyblockPlayerState.get(defenderTarget).maximumHealth() * 0.05F
                        : target.getMaxHealth() * 0.05F;
            } else {
            SkyblockPlayerState attackerState = SkyblockPlayerState.get(attacker);
            SkyblockStats attackStats = SkyblockLevelBonuses.apply(
                    AccessoryBag.applyGemstoneAccessories(attacker,
                            AccessoryBag.applyAdminRing(attacker, attackerState.applyCategoryBonuses(
                                    VanillaWeaponStats.applyHeldWeapon(
                                            ModArmorItems.getSkyblockStats(attacker), attacker.getMainHandItem())))),
                    attackerState.level());
            attackStats = VengenceHandler.applyActiveBuff(attacker, attackStats);
            VanillaWeaponStats.WeaponStats weapon = VanillaWeaponStats.get(attacker.getMainHandItem());
            if (attackStats.weaponDamage() > 0.0) {
                float attackScale = attacker.getAttackStrengthScale(0.5F);
                adjusted = (float) (attackStats.calculateWeaponDamage(false) * attackScale
                        * attackerState.activePet().damageMultiplier()
                        * AccessoryBag.damageBonusMultiplier(attacker));
            }
            adjusted = AccessoryBag.applyTarantulaTalisman(attacker, target, adjusted);

            int chance = Math.min(100, attackStats.critChance());
            if (chance > 0 && attacker.getRandom().nextInt(100) < chance) {
                adjusted *= (float) attackStats.criticalMultiplier();
                hypixelcustomrecipe$criticalHit = true;
            }
            }
        }

        // Players use the custom Health/Defense pool directly. Intercept the hit
        // before vanilla subtracts health or runs its death logic; restoring vanilla
        // health in a RETURN injection is too late when a hit is lethal.
        if (target instanceof ServerPlayer defender) {
            if (!soulBladeAttack && source.getEntity() instanceof LivingEntity attacker && attacker != defender) {
                adjusted *= AccessoryBag.incomingDamageMultiplier(defender, attacker);
            }
            if (adjusted <= 0.0F || defender.isInvulnerableTo(level, source)) {
                // This player hit is handled here; do not let the RETURN hook
                // treat the unchanged vanilla health as damage a second time.
                hypixelcustomrecipe$trackingPlayerDamage = false;
                hypixelcustomrecipe$trackingDamageNumber = false;
                hypixelcustomrecipe$damageNumberAmount = 0.0F;
                cir.setReturnValue(false);
                return;
            }

            SkyblockPlayerState state = SkyblockPlayerState.get(defender);
            dev.hypixelcustomrecipe.NecronGuardHandler.onPlayerHit(defender);
            if (source.getEntity() != null) state.markCombat();

            // Player health is custom and is not subtracted from vanilla health.
            // Calculate the same rounded post-Defense value used by the custom
            // Health pool, then display it above the player who was hit.
            float displayedDamage = (float) (Math.round(
                    (soulBladeAttack ? adjusted : state.stats().applyDefense(adjusted)) * 10.0) / 10.0);
            boolean defeated = soulBladeAttack
                    ? state.applyTrueDamage(adjusted) : state.applyIncomingDamage(adjusted);
            if (displayedDamage > 0.0F) {
                DamageNumberHandler.show(level, defender, displayedDamage,
                        hypixelcustomrecipe$criticalHit);
            }

            // The RETURN hook below is for vanilla-managed mob health. Clear its
            // player tracking flag so it cannot mistake die() setting vanilla
            // health to zero for a second, enormous hit and revive the player.
            hypixelcustomrecipe$trackingPlayerDamage = false;
            hypixelcustomrecipe$trackingDamageNumber = false;
            hypixelcustomrecipe$damageNumberAmount = 0.0F;
            if (defeated) {
                // Commit the vanilla entity to its normal death state before
                // invoking the ServerPlayer death flow. Custom Health is
                // separate from vanilla Health, which otherwise remains at
                // its large reserve value and can leave the player alive but
                // unable to play after reaching zero custom Health.
                defender.setHealth(0.0F);
                defender.die(source);
            }
            cir.setReturnValue(true);
            return;
        }

        // Soul Blade damage is true damage: remove exactly 5% of maximum health,
        // bypassing armor, Defense, Strength, critical hits, and accessory modifiers.
        if (soulBladeAttack) {
            if (target.isInvulnerableTo(level, source)) {
                hypixelcustomrecipe$trackingPlayerDamage = false;
                hypixelcustomrecipe$trackingDamageNumber = false;
                hypixelcustomrecipe$damageNumberAmount = 0.0F;
                cir.setReturnValue(false);
                return;
            }
            target.setHealth(Math.max(0.0F, target.getHealth() - adjusted));
            DamageNumberHandler.show(level, target, adjusted, false);
            if (target.getHealth() <= 0.0F) target.die(source);
            hypixelcustomrecipe$trackingPlayerDamage = false;
            hypixelcustomrecipe$trackingDamageNumber = false;
            hypixelcustomrecipe$damageNumberAmount = 0.0F;
            cir.setReturnValue(true);
            return;
        }

        // Keep the full attack value for the floating number. The target's health
        // loss can be much smaller simply because the mob has little health left.
        hypixelcustomrecipe$damageNumberAmount = adjusted;
        if (Float.compare(adjusted, amount) == 0) return;

        hypixelcustomrecipe$reapplyingDamage.set(true);
        boolean successful;
        try {
            successful = target.hurtServer(level, source, adjusted);
        } finally {
            hypixelcustomrecipe$reapplyingDamage.remove();
        }

        // Cancelling hurtServer at HEAD skips its RETURN injection. Emit the
        // floating number here for attacks whose custom damage differs from vanilla.
        if (successful && hypixelcustomrecipe$trackingDamageNumber
                && hypixelcustomrecipe$damageNumberAmount > 0.0F) {
            DamageNumberHandler.show(level, target, hypixelcustomrecipe$damageNumberAmount,
                    hypixelcustomrecipe$criticalHit);
        }
        hypixelcustomrecipe$trackingDamageNumber = false;
        hypixelcustomrecipe$damageNumberAmount = 0.0F;
        cir.setReturnValue(successful);
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void hypixelcustomrecipe$applyCustomHealth(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (hypixelcustomrecipe$reapplyingDamage.get()) return;

        LivingEntity target = (LivingEntity) (Object) this;
        if (!cir.getReturnValue()) {
            hypixelcustomrecipe$trackingPlayerDamage = false;
            hypixelcustomrecipe$trackingDamageNumber = false;
            hypixelcustomrecipe$damageNumberAmount = 0.0F;
            return;
        }

        // Apply the custom Void status after successful Voidridden attacks.
        if (source.getEntity() instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenZombie) {
            dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEffects.applyVoid(target, 10);
        } else if (source.getEntity() instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenSkeleton
                && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow) {
            dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEffects.applyVoid(target, 5);
        } else if (source.getEntity() instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenCreeper) {
            dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEffects.applyVoid(target, 3);
        }

        if (hypixelcustomrecipe$trackingDamageNumber) {
            hypixelcustomrecipe$trackingDamageNumber = false;
            float dealt = hypixelcustomrecipe$damageNumberAmount;
            hypixelcustomrecipe$damageNumberAmount = 0.0F;
            if (dealt > 0.0F) {
                DamageNumberHandler.show(level, target, dealt, hypixelcustomrecipe$criticalHit);
            }
        }

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target) {
            SkyblockPlayerState.get(attacker).markCombat();
        }

        if (!hypixelcustomrecipe$trackingPlayerDamage) return;
        hypixelcustomrecipe$trackingPlayerDamage = false;

        if (!(target instanceof ServerPlayer defender)) return;

        float actualDamage = Math.max(0.0F, hypixelcustomrecipe$healthBeforeHit - defender.getHealth());
        if (actualDamage <= 0.0F) return;

        dev.hypixelcustomrecipe.NecronGuardHandler.onPlayerHit(defender);

        if (source.getEntity() != null) {
            SkyblockPlayerState.get(defender).markCombat();
        }

        SkyblockPlayerState state = SkyblockPlayerState.get(defender);
        boolean defeated = state.applyIncomingDamage(actualDamage);
        defender.setHealth(Math.min(hypixelcustomrecipe$healthBeforeHit, defender.getMaxHealth()));

        if (defeated) {
            defender.die(source);
        }
    }
    @Inject(method = "die", at = @At("HEAD"))
    private void hypixelcustomrecipe$awardCombatExperience(DamageSource source,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LivingEntity defeated = (LivingEntity) (Object) this;
        if (hypixelcustomrecipe$combatXpAwarded || defeated.level().isClientSide()
                || defeated instanceof Player || !(source.getEntity() instanceof ServerPlayer killer)) return;
        hypixelcustomrecipe$combatXpAwarded = true;
        AccessoryBag.onMobKill(killer);
        VengenceHandler.onMobKill(killer);
        int xp = hypixelcustomrecipe$combatExperienceFor(defeated)
                + AccessoryBag.combatXpBonus(killer);
        if (xp > 0) {
            SkyblockPlayerState.get(killer).addCombatExperience(killer, xp);
        }
    }

    @Unique
    private static int hypixelcustomrecipe$combatExperienceFor(LivingEntity entity) {
        if (entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenSkeleton
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenSpider
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenZombie) return 200;
        if (entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenCreeper
                || entity instanceof dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEnderman) return 300;
        if (entity instanceof EnderDragon) return 5000;
        if (entity instanceof WitherBoss) return 1000;
        if (entity instanceof Warden) return 7500;
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(entity.getType()).getPath();
        if (id.equals("shulker") || id.equals("ravager")) return 75;
        if (id.equals("slime") || id.equals("evoker") || id.equals("blaze")
                || id.equals("ghast") || id.equals("wither_skeleton")) return 30;
        if (id.equals("vindicator") || id.equals("illusioner") || id.equals("vex")
                || id.equals("magma_cube") || id.equals("enderman")) return 12;
        if (id.equals("zombie") || id.equals("skeleton") || id.equals("spider")
                || id.equals("pillager") || id.equals("creeper") || id.equals("witch")
                || id.equals("silverfish")) return 5;
        if (id.equals("cow") || id.equals("pig") || id.equals("sheep")
                || id.equals("chicken") || id.equals("rabbit") || id.equals("mooshroom")
                || id.equals("goat")) return 2;
        return 0;
    }

}
