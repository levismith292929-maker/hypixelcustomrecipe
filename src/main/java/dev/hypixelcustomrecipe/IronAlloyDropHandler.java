package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class IronAlloyDropHandler {
    private IronAlloyDropHandler() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.getType() != EntityTypes.IRON_GOLEM
                    || entity.getRandom().nextFloat() >= 0.025F
                    || !(entity.level() instanceof ServerLevel level)) {
                return;
            }

            ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5,
                    entity.getZ(), new ItemStack(DatapackItems.IRON_ALLOY));
            level.addFreshEntity(drop);
        });
    }
}
