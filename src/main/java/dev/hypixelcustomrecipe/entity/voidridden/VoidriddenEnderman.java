package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public final class VoidriddenEnderman extends EnderMan {
    private int extraTeleportCooldown = 40;
    public VoidriddenEnderman(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        setHealth(getMaxHealth());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (getTarget() != null && --extraTeleportCooldown <= 0) {
            teleportRandomly();
            extraTeleportCooldown = 40;
        }
    }
}
