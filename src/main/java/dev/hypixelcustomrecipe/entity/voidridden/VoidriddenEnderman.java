package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

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
            Vec3 trailStart = new Vec3(getX(), getY() + 1.0, getZ());
            teleport();
            Vec3 trailEnd = new Vec3(getX(), getY() + 1.0, getZ());
            if (trailStart.distanceToSqr(trailEnd) > 1.0) {
                VoidTrailManager.add(level, getUUID(), trailStart, trailEnd);
            }
            extraTeleportCooldown = 40;
        }
    }
}
