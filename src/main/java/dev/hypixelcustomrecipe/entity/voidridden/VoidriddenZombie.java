package dev.hypixelcustomrecipe.entity.voidridden;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.EntityTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class VoidriddenZombie extends Zombie implements PolymerEntity {
    public VoidriddenZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);


        setHealth(getMaxHealth());
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.ZOMBIE;
    }


    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 80 != 0
                || !(level() instanceof ServerLevel serverLevel)
                || !(getTarget() instanceof Player target) || !target.isAlive()) {
            return;
        }

        VoidballEntity voidball = new VoidballEntity(VoidriddenMobs.VOIDBALL, serverLevel);
        voidball.setOwner(this);
        voidball.setPos(getX(), getEyeY() - 0.1, getZ());

        double dx = target.getX() - getX();
        double dy = target.getEyeY() - getEyeY();
        double dz = target.getZ() - getZ();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 1.0E-6) {
            voidball.setDeltaMovement(dx / length * 1.35, dy / length * 1.35, dz / length * 1.35);
        }
        serverLevel.addFreshEntity(voidball);
    }
}
