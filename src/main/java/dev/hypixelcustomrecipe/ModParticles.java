package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class ModParticles {
    public static final SimpleParticleType CRIT = FabricParticleTypes.simple();

    private ModParticles() {
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(HypixelCustomRecipe.MOD_ID, "crit"), CRIT);
    }
}
