package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the live per-player SkyBlock state on the logical server. */
public final class SkyblockPlayerState {
    public enum Pet {
        NONE(500, 40, 0.0),
        ENDERMAN(1000, 50, -0.20),
        ENDER_DRAGON(300, 20, 0.15),
        GOLDEN_DRAGON(750, 10, 0.40),
        IRON_GOLEM(600, 30, 0.05);

        private final int manaMaximum;
        private final int manaRegeneration;
        private final double damageBonus;

        Pet(int manaMaximum, int manaRegeneration, double damageBonus) {
            this.manaMaximum = manaMaximum;
            this.manaRegeneration = manaRegeneration;
            this.damageBonus = damageBonus;
        }

        public int manaMaximum() { return manaMaximum; }
        public int manaRegeneration() { return manaRegeneration; }
        public double damageMultiplier() { return 1.0 + damageBonus; }
    }

    private static final Map<UUID, SkyblockPlayerState> PLAYERS = new ConcurrentHashMap<>();
    private final SkyblockManaState mana = new SkyblockManaState();
    private SkyblockStats stats = SkyblockStats.BASE;
    private Pet activePet = Pet.NONE;
    private int hudUpdateTicks;
    private int astraeaOrbTicks;
    private int currentHealth;
    private int maximumHealth;
    private boolean healthInitialized;

    private SkyblockPlayerState() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) get(player).tick(player);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PLAYERS.clear());
    }

    public static SkyblockPlayerState get(Player player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new SkyblockPlayerState());
    }

    public SkyblockStats stats() { return stats; }
    public SkyblockManaState mana() { return mana; }
    public Pet activePet() { return activePet; }
    public int currentHealth() { return currentHealth; }
    public int maximumHealth() { return maximumHealth; }

    /** Subtracts post-vanilla-mitigation damage using the custom Defense formula. */
    public boolean applyIncomingDamage(float vanillaDamage) {
        int effective = (int) Math.round(stats.applyDefense(vanillaDamage) * 10.0);
        if (effective > 0) currentHealth = Math.max(0, currentHealth - effective);
        return currentHealth <= 0;
    }

    /** Restores custom Health points; one vanilla heart is 20 custom points. */
    public void heal(int amount) {
        if (amount > 0) currentHealth = Math.min(maximumHealth, currentHealth + amount);
    }

    public void startAstraeaOrbs() { astraeaOrbTicks = 100; }

    public void activatePet(ServerPlayer player, Pet pet) {
        if (activePet == Pet.IRON_GOLEM) player.removeEffect(MobEffects.RESISTANCE);
        activePet = pet;
        if (pet == Pet.IRON_GOLEM) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
        }
        mana.configure(pet.manaMaximum() + stats.intelligence(),
                pet.manaRegeneration() + Math.max(0, (stats.intelligence() + 2) / 4));
    }


    /**
     * The datapack removes vanilla armor attributes and calculates Defense
     * through its own stat formula instead. Keep that behavior in the mod so
     * vanilla armor does not apply a second, separate armor reduction.
     */
    private static void stripVanillaArmorAttributes(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                if (modifiers == null || !modifiers.equals(ItemAttributeModifiers.EMPTY)) {
                    stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
                }
            }
        }
    }

    private void tick(ServerPlayer player) {
        stripVanillaArmorAttributes(player);
        stats = VanillaWeaponStats.applyHeldWeapon(ModArmorItems.getSkyblockStats(player), player.getMainHandItem());

        int recalculatedMaximum = Math.max(1, stats.health() * 10);
        if (!healthInitialized) {
            maximumHealth = recalculatedMaximum;
            currentHealth = maximumHealth;
            healthInitialized = true;
        } else if (currentHealth <= 0 && player.isAlive()) {
            maximumHealth = recalculatedMaximum;
            currentHealth = maximumHealth;
        } else {
            int healthDifference = recalculatedMaximum - maximumHealth;
            maximumHealth = recalculatedMaximum;
            currentHealth = Math.max(1, Math.min(maximumHealth, currentHealth + healthDifference));
        }

        // The custom Health pool is authoritative. Keep vanilla health as a
        // large reserve so normal damage can be measured after vanilla
        // resistance, shields, and other mitigation have run.
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
        if (player.getHealth() < 1024.0F) player.setHealth(1024.0F);

        mana.configure(activePet.manaMaximum() + stats.intelligence(),
                activePet.manaRegeneration() + Math.max(0, (stats.intelligence() + 2) / 4));
        mana.tick();

        if (astraeaOrbTicks > 0) {
            ServerLevel level = (ServerLevel) player.level();
            double rotation = (100 - astraeaOrbTicks) * 0.12;
            for (int i = 0; i < 3; i++) {
                double angle = rotation + i * (Math.PI * 2.0 / 3.0);
                double x = player.getX() + Math.cos(angle) * 1.0;
                double y = player.getY() + 1.0 + Math.sin(angle * 2.0) * 0.2;
                double z = player.getZ() + Math.sin(angle) * 1.0;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
            }
            astraeaOrbTicks--;
        }

        if (++hudUpdateTicks >= 10) {
            hudUpdateTicks = 0;
            String hud = String.format(java.util.Locale.ROOT,
                    "Health: %.1f/%.1f | Defense: %d | Mana: %d/%d",
                    currentHealth / 10.0, maximumHealth / 10.0, stats.defense(),
                    mana.currentMana(), mana.maximumMana());
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(hud)));
        }
    }
}
