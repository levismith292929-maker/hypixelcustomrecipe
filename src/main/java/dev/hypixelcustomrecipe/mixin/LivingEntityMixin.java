package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.DamageNumberHandler;
import dev.hypixelcustomrecipe.ModArmorItems;
import dev.hypixelcustomrecipe.SkyblockPlayerState;
import dev.hypixelcustomrecipe.SkyblockStats;
import dev.hypixelcustomrecipe.SkyblockLevelBonuses;
import dev.hypixelcustomrecipe.VanillaWeaponStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    private boolean hypixelcustomrecipe$criticalHit;

    @Unique
    private float hypixelcustomrecipe$damageNumberAmount;

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

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target
                && source.getDirectEntity() == attacker) {
            SkyblockStats attackStats = SkyblockLevelBonuses.apply(
                    VanillaWeaponStats.applyHeldWeapon(
                            ModArmorItems.getSkyblockStats(attacker), attacker.getMainHandItem()),
                    SkyblockPlayerState.get(attacker).level());
            VanillaWeaponStats.WeaponStats weapon = VanillaWeaponStats.get(attacker.getMainHandItem());
            if (weapon.damage() > 0) {
                float attackScale = attacker.getAttackStrengthScale(0.5F);
                adjusted = (float) (attackStats.calculateWeaponDamage(false) * attackScale);
            }

            int chance = Math.min(100, attackStats.critChance());
            if (chance > 0 && attacker.getRandom().nextInt(100) < chance) {
                adjusted *= (float) attackStats.criticalMultiplier();
                hypixelcustomrecipe$criticalHit = true;
            }
        }

        // Keep the full attack value for the floating number. The target's health
        // loss can be much smaller simply because the mob has little health left.
        hypixelcustomrecipe$damageNumberAmount = adjusted;
        if (Float.compare(adjusted, amount) == 0) return;

        hypixelcustomrecipe$reapplyingDamage.set(true);
        try {
            cir.setReturnValue(target.hurtServer(level, source, adjusted));
        } finally {
            hypixelcustomrecipe$reapplyingDamage.remove();
        }
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
}
