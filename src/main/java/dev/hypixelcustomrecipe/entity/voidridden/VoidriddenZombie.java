package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public final class VoidriddenZombie extends Zombie {
    public VoidriddenZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }
}
