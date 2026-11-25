package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.SkyblockPlayerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds earned vanilla XP levels into the separate SkyBlock level track. */
@Mixin(Player.class)
public abstract class PlayerExperienceMixin {
    @Inject(method = "giveExperienceLevels", at = @At("TAIL"))
    private void hypixelcustomrecipe$trackExperience(int amount, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player instanceof ServerPlayer serverPlayer && amount > 0) {
            SkyblockPlayerState.get(serverPlayer).addLevelExperience(serverPlayer, amount);
        }
    }
}
