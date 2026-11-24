package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.ModArmorItems;
import dev.hypixelcustomrecipe.SkyblockPlayerState;
import dev.hypixelcustomrecipe.SkyblockStats;
import dev.hypixelcustomrecipe.VanillaWeaponStats;
import net.minecraft.core.particles.ParticleTypes;
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

    /**
     * Replaces vanilla sword/axe damage with the migrated weapon stats and
     * applies the SkyBlock critical-hit formula. Player damage is measured
     * after vanilla mitigation in the RETURN injection below.
     */
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$applySkyblockAttack(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (hypixelcustomrecipe$reapplyingDamage.get() || amount <= 0.0F) return;

        LivingEntity target = (LivingEntity) (Object) this;
        float adjusted = amount;

        if (target instanceof ServerPlayer defender) {
            hypixelcustomrecipe$healthBeforeHit = defender.getHealth();
            hypixelcustomrecipe$trackingPlayerDamage = true;
        }

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target
                && source.getDirectEntity() == attacker) {
            SkyblockStats attackStats = VanillaWeaponStats.applyHeldWeapon(
                    ModArmorItems.getSkyblockStats(attacker), attacker.getMainHandItem()
            );
            VanillaWeaponStats.WeaponStats weapon = VanillaWeaponStats.get(attacker.getMainHandItem());
            if (weapon.damage() > 0) {
                float attackScale = attacker.getAttackStrengthScale(0.5F);
                adjusted = (float) (attackStats.calculateWeaponDamage(false) * attackScale);
            }

            int chance = Math.min(100, attackStats.critChance());
            if (chance > 0 && attacker.getRandom().nextInt(100) < chance) {
                adjusted *= (float) attackStats.criticalMultiplier();
                level.sendParticles(
                        ParticleTypes.FLAME,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.5,
                        target.getZ(),
                        8, 0.22, 0.22, 0.22, 0.015
                );
            }
        }

        if (Float.compare(adjusted, amount) == 0) return;

        hypixelcustomrecipe$reapplyingDamage.set(true);
        try {
            cir.setReturnValue(target.hurtServer(level, source, adjusted));
        } finally {
            hypixelcustomrecipe$reapplyingDamage.remove();
        }
    }

    /**
     * The datapack keeps vanilla health as a reserve and uses a custom Health
     * pool. Measure actual health lost after vanilla mitigation, restore the
     * reserve, and apply Defense to the custom pool.
     */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void hypixelcustomrecipe$applyCustomHealth(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!hypixelcustomrecipe$trackingPlayerDamage) return;
        hypixelcustomrecipe$trackingPlayerDamage = false;

        LivingEntity target = (LivingEntity) (Object) this;
        if (!(target instanceof ServerPlayer defender) || !cir.getReturnValue()) return;

        float actualDamage = Math.max(0.0F, hypixelcustomrecipe$healthBeforeHit - defender.getHealth());
        if (actualDamage <= 0.0F) return;

        SkyblockPlayerState state = SkyblockPlayerState.get(defender);
        boolean defeated = state.applyIncomingDamage(actualDamage);
        defender.setHealth(Math.min(hypixelcustomrecipe$healthBeforeHit, defender.getMaxHealth()));

        if (defeated) {
            defender.die(source);
        }
    }
}
