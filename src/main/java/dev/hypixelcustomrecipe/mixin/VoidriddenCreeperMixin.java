package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenCreeper;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Creeper.class)
public abstract class VoidriddenCreeperMixin {
    @Shadow private int maxSwell;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hypixelcustomrecipe$lengthenVoidFuse(EntityType<? extends Creeper> type, Level level, CallbackInfo ci) {
        if ((Object) this instanceof VoidriddenCreeper) {
            maxSwell = 60;
        }
    }

    @Invoker("explodeCreeper")
    protected abstract void hypixelcustomrecipe$invokeVanillaExplosion();

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Creeper;explodeCreeper()V"
            )
    )
    private void hypixelcustomrecipe$replaceExplosion(Creeper creeper) {
        if (creeper instanceof VoidriddenCreeper voidridden) {
            voidridden.explodeVoid();
        } else {
            ((VoidriddenCreeperMixin) (Object) creeper).hypixelcustomrecipe$invokeVanillaExplosion();
        }
    }
}
