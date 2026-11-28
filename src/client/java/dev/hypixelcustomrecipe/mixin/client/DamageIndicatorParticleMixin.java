package dev.hypixelcustomrecipe.mixin.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes vanilla damage-indicator particles (the little heart-like particles)
 * so rapid custom damage hits do not fill the screen with them.
 */
@Mixin(ParticleEngine.class)
public abstract class DamageIndicatorParticleMixin {
    @Inject(method = "makeParticle", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$hideDamageIndicators(
            ParticleOptions options,
            double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            CallbackInfoReturnable<Particle> cir
    ) {
        if (options == ParticleTypes.DAMAGE_INDICATOR) {
            cir.setReturnValue(null);
        }
    }
}
