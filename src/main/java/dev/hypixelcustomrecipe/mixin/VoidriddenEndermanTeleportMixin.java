package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEnderman;
import dev.hypixelcustomrecipe.entity.voidridden.VoidTrailManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class VoidriddenEndermanTeleportMixin {
    @Unique
    private double hypixelcustomrecipe$oldX;
    @Unique
    private double hypixelcustomrecipe$oldY;
    @Unique
    private double hypixelcustomrecipe$oldZ;

    @Inject(method = "randomTeleport(DDDZ)Z", at = @At("HEAD"))
    private void hypixelcustomrecipe$captureTeleportStart(
            double x, double y, double z, boolean showParticles, CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof VoidriddenEnderman enderman) {
            hypixelcustomrecipe$oldX = enderman.getX();
            hypixelcustomrecipe$oldY = enderman.getY();
            hypixelcustomrecipe$oldZ = enderman.getZ();
        }
    }

    @Inject(method = "randomTeleport(DDDZ)Z", at = @At("RETURN"))
    private void hypixelcustomrecipe$createVoidTrail(
            double x, double y, double z, boolean showParticles, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())
                || !((Object) this instanceof VoidriddenEnderman enderman)
                || !(enderman.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 start = new Vec3(hypixelcustomrecipe$oldX, hypixelcustomrecipe$oldY + 1.0, hypixelcustomrecipe$oldZ);
        Vec3 end = new Vec3(enderman.getX(), enderman.getY() + 1.0, enderman.getZ());
        VoidTrailManager.add(serverLevel, enderman.getUUID(), start, end);
    }
}
