package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;

public final class VoidriddenEnderman extends EnderMan {
    public VoidriddenEnderman(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        setHealth(getMaxHealth());
    }

}
