package dev.hypixelcustomrecipe.entity.voidridden;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.EntityTypes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

public final class VoidriddenSkeleton extends Skeleton implements PolymerEntity {
    public VoidriddenSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.SKELETON;
    }
        setHealth(getMaxHealth());
    }


    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        // Fire five arrows in the same attack, preserving the vanilla arrow logic.
        for (int i = 0; i < 5; i++) {
            super.performRangedAttack(target, power);
        }
    }
}
