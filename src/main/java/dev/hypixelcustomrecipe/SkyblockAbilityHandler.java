package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * First migrated item-ability handler. Abilities are activated directly from
 * Java item-use events; no datapack advancement trigger is required.
 */
public final class SkyblockAbilityHandler {
    private SkyblockAbilityHandler() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (stack.is(ModItems.AOTE)) {
                useAote(serverPlayer);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    /**
     * Aspect of the End: spend 35 mana, teleport five blocks forward,
     * clear vertical velocity, and use a four-tick cooldown.
     */
    private static void useAote(ServerPlayer player) {
        SkyblockManaState mana = SkyblockPlayerState.get(player).mana();
        if (mana.isOnCooldown()) {
            return;
        }
        if (!mana.spend(35)) {
            player.sendSystemMessage(Component.literal("Not enough mana! (35 required)"));
            return;
        }

        mana.setRegenerationDelay(40);
        mana.startCooldown(4);

        Vec3 destination = player.position().add(player.getLookAngle().scale(5.0));
        player.teleportTo(destination.x, destination.y, destination.z);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, 0.0, velocity.z);
        player.fallDistance = 0.0F;
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.SHULKER_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
