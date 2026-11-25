package dev.hypixelcustomrecipe.entity.voidridden;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.EntityHitResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class VoidballEntity extends Snowball {
    public VoidballEntity(EntityType<? extends Snowball> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ENDER_PEARL;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(level() instanceof ServerLevel serverLevel)
                || !(hitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (getOwner() instanceof LivingEntity owner) {
            target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(owner), 500.0F);
        } else {
            target.hurtServer(serverLevel, serverLevel.damageSources().generic(), 500.0F);
        }
        VoidriddenEffects.applyVoid(target, 10);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!level().isClientSide()) {
            discard();
        }
    }
}
