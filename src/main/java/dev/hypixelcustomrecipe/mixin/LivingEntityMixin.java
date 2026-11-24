package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.SkyblockPlayerState;
import dev.hypixelcustomrecipe.SkyblockStats;
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

    /**
     * Applies the datapack's Defense formula to player damage and the custom
     * critical formula to player attacks. Re-entry is guarded so the adjusted
     * amount is applied exactly once.
     */
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$applySkyblockCombat(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (hypixelcustomrecipe$reapplyingDamage.get() || amount <= 0.0F) return;

        LivingEntity target = (LivingEntity) (Object) this;
        float adjusted = amount;

        if (target instanceof ServerPlayer defender) {
            SkyblockStats defenseStats = SkyblockPlayerState.get(defender).stats();
            adjusted = (float) defenseStats.applyDefense(adjusted);
        }

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target) {
            SkyblockStats attackStats = SkyblockPlayerState.get(attacker).stats();
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
}
