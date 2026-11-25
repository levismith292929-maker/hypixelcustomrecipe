package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public final class VoidriddenZombie extends Zombie {
    public VoidriddenZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
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
        voidball.shoot(dx, dy, dz, 1.35F, 0.0F);
        serverLevel.addFreshEntity(voidball);
    }
}
