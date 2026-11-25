package dev.hypixelcustomrecipe.entity.voidridden;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.EntityTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

public final class VoidriddenSpider extends Spider implements PolymerEntity {
    public VoidriddenSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.SPIDER;
    }
        setHealth(getMaxHealth());
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
