package dev.hypixelcustomrecipe.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents a Necron guard from ever selecting the player who summoned it. */
@Mixin(Mob.class)
public abstract class NecronGuardTargetMixin {
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$protectGuardOwner(
            LivingEntity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Mob mob = (Mob) (Object) this;
        if (!mob.getTags().contains("hypixel_necron_guard")) {
            return;
        }

        if (mob.getTags().contains("hypixel_necron_guard_owner_" + target.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
