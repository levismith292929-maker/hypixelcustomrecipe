package dev.hypixelcustomrecipe.entity.voidridden;

import dev.hypixelcustomrecipe.HypixelCustomRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;

public final class VoidriddenEffects {
    public static final Holder<MobEffect> VOID = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(HypixelCustomRecipe.MOD_ID, "void"),
            new VoidEffect()
    );

    private VoidriddenEffects() {
    }

    public static void initialize() {
        HypixelCustomRecipe.LOGGER.info("Registered the Void status effect.");
    }

    public static void applyVoid(LivingEntity entity, int seconds) {
        if (entity.level() instanceof ServerLevel) {
            entity.addEffect(new MobEffectInstance(VOID, seconds * 20, 0, false, false, true));
        }
    }

    private static final class VoidEffect extends MobEffect {
        private VoidEffect() {
            super(MobEffectCategory.HARMFUL, 0x080808);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            // Out-of-world damage bypasses armor and ordinary damage reduction.
            entity.hurtServer(level, level.damageSources().outOfWorld(), 25.0F);
            entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 40, 0, true, false, false));
            return true;
        }
    }
}
