package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.SkyblockPlayerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Saves the migrated SkyBlock state alongside the player's normal data. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void hypixelcustomrecipe$saveSkyblockState(ValueOutput output, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        SkyblockPlayerState.get(player).savePersistentData(player, output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void hypixelcustomrecipe$loadSkyblockState(ValueInput input, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        SkyblockPlayerState.get(player).loadPersistentData(player, input);
    }
}
