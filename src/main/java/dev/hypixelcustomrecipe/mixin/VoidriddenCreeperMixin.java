package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenCreeper;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Creeper.class)
public abstract class VoidriddenCreeperMixin {
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
