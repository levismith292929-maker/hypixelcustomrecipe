package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class VoidriddenEnderman extends EnderMan {
    private int extraTeleportCooldown = 40;
    private boolean hasBeenHitByPlayer;

    public VoidriddenEnderman(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        setHealth(getMaxHealth());
    }

    public boolean hasBeenHitByPlayer() {
        return hasBeenHitByPlayer;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player) {
            hasBeenHitByPlayer = true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (getTarget() != null && --extraTeleportCooldown <= 0) {
            teleport();
            extraTeleportCooldown = 40;
        }
    }
}
