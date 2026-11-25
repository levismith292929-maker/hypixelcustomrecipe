package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

public final class VoidriddenSpider extends Spider {
    public VoidriddenSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        setHealth(getMaxHealth());
    }

    @Override
    public float getMaxHealth() {
        return 80000.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 20 != 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (Player player : serverLevel.getEntitiesOfClass(Player.class, getBoundingBox().inflate(2.0))) {
            if (player.isAlive()) {
                player.hurtServer(serverLevel, serverLevel.damageSources().fellOutOfWorld(), 10.0F);
            }
        }
    }
}
