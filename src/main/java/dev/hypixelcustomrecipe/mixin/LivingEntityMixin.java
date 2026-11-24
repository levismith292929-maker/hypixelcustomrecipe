package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.ModArmorItems;
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
    private static final ThreadLocal<Boolean> hypixelcustomrecipe$insideCriticalDamage =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$applyCriticalHit(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (hypixelcustomrecipe$insideCriticalDamage.get() || amount <= 0.0F) {
            return;
        }

        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        int critChance = ModArmorItems.getCriticalChance(attacker);
        if (attacker.getRandom().nextInt(100) >= critChance) {
            return;
        }

        int critDamage = ModArmorItems.getCriticalDamage(attacker);
        float multiplier = 3.0F + (critDamage / 50.0F);

        LivingEntity target = (LivingEntity) (Object) this;
        level.sendParticles(
                ParticleTypes.FLAME,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                8,
                0.22,
                0.22,
                0.22,
                0.015
        );

        hypixelcustomrecipe$insideCriticalDamage.set(true);
        try {
            cir.setReturnValue(target.hurtServer(level, source, amount * multiplier));
        } finally {
            hypixelcustomrecipe$insideCriticalDamage.remove();
        }
    }
}
