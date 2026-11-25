package dev.hypixelcustomrecipe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

public final class HypixelCustomRecipeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticleProviderRegistry.getInstance().register(ModParticles.CRIT, CritParticle.Provider::new);
    }
}
