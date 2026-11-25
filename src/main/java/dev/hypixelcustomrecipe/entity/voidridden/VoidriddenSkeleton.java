package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

public final class VoidriddenSkeleton extends Skeleton {
    public VoidriddenSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        setHealth(getMaxHealth());
    }

    @Override
    public float getMaxHealth() {
        return 100000.0F;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        // Fire five arrows in the same attack, preserving the vanilla arrow logic.
        for (int i = 0; i < 5; i++) {
            super.performRangedAttack(target, power);
        }
    }
}
