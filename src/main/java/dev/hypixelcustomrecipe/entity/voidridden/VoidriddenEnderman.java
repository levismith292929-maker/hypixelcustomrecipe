package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;\nimport net.minecraft.world.entity.player.Player;\nimport net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class VoidriddenEnderman extends EnderMan {
    private int extraTeleportCooldown = 40;\n    private boolean hasBeenHitByPlayer;\n\n    public boolean hasBeenHitByPlayer() {\n        return hasBeenHitByPlayer;\n    }\n\n    @Override\n    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {\n        if (source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player) {\n            hasBeenHitByPlayer = true;\n        }\n        return super.hurtServer(level, source, amount);\n    }
    public VoidriddenEnderman(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        setHealth(getMaxHealth());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (getTarget() != null && --extraTeleportCooldown <= 0) {
            teleport();\n            extraTeleportCooldown = 40;
        }
    }
}
