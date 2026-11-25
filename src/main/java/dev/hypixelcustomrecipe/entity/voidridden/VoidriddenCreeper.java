package dev.hypixelcustomrecipe.entity.voidridden;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.EntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

public final class VoidriddenCreeper extends Creeper implements PolymerEntity {
    public VoidriddenCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);


        setHealth(getMaxHealth());
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.CREEPER;
    }


    public void explodeVoid() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double x = getX();
        double y = getY();
        double z = getZ();

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 4.0F, 0.8F);

        double radiusSquared = 15.0 * 15.0;
        for (LivingEntity target : serverLevel.getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(15.0)
        )) {
            if (target == this || !target.isAlive()
                    || target.distanceToSqr(x, y, z) > radiusSquared) {
                continue;
            }
            target.hurtServer(serverLevel, serverLevel.damageSources().explosion(this, this), 300.0F);
            VoidriddenEffects.applyVoid(target, 3);
        }

        discard();
    }
}
