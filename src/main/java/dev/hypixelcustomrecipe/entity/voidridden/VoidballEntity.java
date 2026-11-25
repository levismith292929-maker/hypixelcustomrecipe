package dev.hypixelcustomrecipe.entity.voidridden;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.EntityTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class VoidballEntity extends DragonFireball implements PolymerEntity {
    public VoidballEntity(EntityType<? extends DragonFireball> type, Level level) {
        super(type, level);


    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.DRAGON_FIREBALL;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (level() instanceof ServerLevel serverLevel
                && hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target) {
            if (getOwner() instanceof LivingEntity owner) {
                target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(owner), 500.0F);
            } else {
                target.hurtServer(serverLevel, serverLevel.damageSources().generic(), 500.0F);
            }
            VoidriddenEffects.applyVoid(target, 10);
        }

        // This projectile uses the dragon-fireball appearance, but deliberately
        // has no vanilla explosion so it cannot damage terrain.
        if (!level().isClientSide()) {
            discard();
        }
    }
}
