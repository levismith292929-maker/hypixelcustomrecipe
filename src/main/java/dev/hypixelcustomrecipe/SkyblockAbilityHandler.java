package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResultHolder;

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
                return InteractionResultHolder.pass(stack);
            }

            if (stack.is(ModItems.AOTE)) {
                useAote(serverPlayer);
                return InteractionResultHolder.success(stack);
            }

            return InteractionResultHolder.pass(stack);
        });
    }

    /**
     * Aspect of the End: spend 35 mana, teleport five blocks forward,
     * apply one second of slow falling, and use a four-tick cooldown.
     * The vertical velocity is explicitly cleared after teleporting.
     */
    private static void useAote(ServerPlayer player) {
        SkyblockManaState mana = SkyblockPlayerState.get(player).mana();
        if (mana.isOnCooldown()) {
            return;
        }
        if (!mana.spend(35)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Not enough mana! (35 required)"), true);
            return;
        }

        mana.setRegenerationDelay(40);
        mana.startCooldown(4);

        Vec3 destination = player.position().add(player.getLookAngle().scale(5.0));
        player.teleportTo(destination.x, destination.y, destination.z);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, 0.0, velocity.z);
        player.fallDistance = 0.0F;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 20, 0, false, false, true));
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.SHULKER_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
