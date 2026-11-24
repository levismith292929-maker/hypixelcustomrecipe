package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.component.ResolvableProfile;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
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
    private final ServerBossEvent manaBossBar = new ServerBossEvent(
            UUID.randomUUID(),
            Component.literal("Mana: 500 / 500"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS
    );
    private SkyblockStats stats = SkyblockStats.BASE;
    private Pet activePet = Pet.NONE;
    private int hudUpdateTicks;
    private int astraeaOrbTicks;
    private int currentHealth;
    private int maximumHealth;
    private boolean healthInitialized;

    private ArmorStand petDisplay;

    private SkyblockPlayerState() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) get(player).tick(player);
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            newPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
            newPlayer.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
            newPlayer.setHealth(1024.0F);
            SkyblockPlayerState state = PLAYERS.get(oldPlayer.getUUID());
            if (state != null) {
                state.manaBossBar.removePlayer(oldPlayer);
                state.manaBossBar.addPlayer(newPlayer);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SkyblockPlayerState state = PLAYERS.get(handler.player.getUUID());
            if (state != null) {
                state.manaBossBar.removePlayer(handler.player);
                state.removePetDisplay();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PLAYERS.values().forEach(state -> {
                state.removePetDisplay();
                state.manaBossBar.removeAllPlayers();
            });
            PLAYERS.clear();
        });
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


    public void savePersistentData(ValueOutput output) {
        output.putInt("hypixel_health", currentHealth);
        output.putInt("hypixel_max_health", maximumHealth);
        output.putInt("hypixel_pet", activePet.ordinal());
        mana.savePersistentData(output);
    }

    public void loadPersistentData(ServerPlayer player, ValueInput input) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        if (player.isAlive()) player.setHealth(1024.0F);

        int savedMaximum = input.getIntOr("hypixel_max_health", 0);
        int savedHealth = input.getIntOr("hypixel_health", -1);
        if (savedMaximum > 0 && savedHealth >= 0) {
            maximumHealth = savedMaximum;
            currentHealth = Math.min(savedHealth, savedMaximum);
            healthInitialized = true;
        }

        mana.loadPersistentData(input);
        int petIndex = input.getIntOr("hypixel_pet", 0);
        Pet[] pets = Pet.values();
        if (petIndex >= 0 && petIndex < pets.length) {
            activePet = pets[petIndex];

        }
    }

    public void startAstraeaOrbs() { astraeaOrbTicks = 100; }


    private int maximumManaFor(Pet pet) {
        int armorBonus = stats.manaMaximum() - SkyblockStats.BASE.manaMaximum();
        return Math.max(1, pet.manaMaximum() + armorBonus + stats.intelligence());
    }

    private int manaRegenerationFor(Pet pet) {
        int armorBonus = stats.manaRegeneration() - SkyblockStats.BASE.manaRegeneration();
        return Math.max(0, pet.manaRegeneration() + armorBonus
                + Math.max(0, (stats.intelligence() + 2) / 4));
    }

    public void activatePet(ServerPlayer player, Pet pet) {
        if (activePet == Pet.IRON_GOLEM) player.removeEffect(MobEffects.RESISTANCE);
        removePetDisplay();
        activePet = pet;
        if (pet == Pet.IRON_GOLEM) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
        }
        mana.configure(maximumManaFor(pet), manaRegenerationFor(pet));
        if (pet != Pet.NONE) spawnPetDisplay(player, pet);
    }

    private void spawnPetDisplay(ServerPlayer player, Pet pet) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        switch (pet) {
            case ENDERMAN -> head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved("MHF_Enderman"));
            case IRON_GOLEM -> head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved("MHF_Golem"));
            case ENDER_DRAGON -> setHeadTexture(head,
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWVjM2ZmNTYzMjkwYjEzZmYzYmNjMzY4OThhZjdlYWE5ODhiNmNjMThkYzI1NDE0N2Y1ODM3NGFmZTliMjFiOSJ9fX0=");
            case GOLDEN_DRAGON -> setHeadTexture(head,
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmU5ZjliMWZjMDE0MTY2Y2I0NmEwOTNlNTM0OWIyYmY2ZWRkMjAxYjY4MGQ2MmU0OGRiZjNhZjliMDQ1OTExNiJ9fX0=");
            default -> { return; }
        }

        ServerLevel level = (ServerLevel) player.level();
        petDisplay = new ArmorStand(level, player.getX(), player.getY() - 0.6, player.getZ());
        petDisplay.setInvisible(true);
        petDisplay.setNoGravity(true);
        petDisplay.setNoPhysics(true);
        petDisplay.setNoBasePlate(true);
        petDisplay.setInvulnerable(true);
        petDisplay.addTag("hypixel_pet_display");
        petDisplay.setItemSlot(EquipmentSlot.HEAD, head);
        level.addFreshEntity(petDisplay);
    }

    private static void setHeadTexture(ItemStack head, String texture) {
        Property textureProperty = new Property("textures", texture);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", textureProperty));
        GameProfile profile = new GameProfile(UUID.randomUUID(), "SkyBlockPet", properties);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
    }

    private void removePetDisplay() {
        if (petDisplay != null) {
            petDisplay.discard();
            petDisplay = null;
        }
    }

    private void updatePetDisplay(ServerPlayer player) {
        if (activePet == Pet.NONE) return;
        if (petDisplay == null || petDisplay.isRemoved() || petDisplay.level() != player.level()) {
            removePetDisplay();
            spawnPetDisplay(player, activePet);
            return;
        }
        double bob = Math.sin(player.tickCount * 0.12) * 0.12;
        petDisplay.setPos(player.getX(), player.getY() - 0.6 + bob, player.getZ());
        petDisplay.setYRot(player.getYRot());
    }


    /**
     * The datapack removes vanilla armor attributes and calculates Defense
     * through its own stat formula instead. Keep that behavior in the mod so
     * vanilla armor does not apply a second, separate armor reduction.
     */


    /** Shows the datapack's end-rod preview at the block AOTV would target. */
    private static void renderAotvPreview(ServerPlayer player) {
        if (!player.getMainHandItem().is(DatapackItems.AOTV)) return;
        var hit = player.pick(64.0, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            var pos = blockHit.getBlockPos();
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    2, 0.15, 0.0, 0.15, 0.01);
        }
    }

    private String abilityCooldownHud(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        String ability = null;
        String label = null;
        if (held.is(ModItems.AOTE)) { ability = "aote"; label = "AOTE"; }
        else if (held.is(DatapackItems.AOTV)) { ability = "aotv"; label = "AOTV"; }
        else if (held.is(DatapackItems.HYPERION)) { ability = "hyperion"; label = "Hyperion"; }
        else if (held.is(DatapackItems.ASTRAEA)) { ability = "astraea"; label = "Astraea"; }
        else if (held.is(DatapackItems.AOTD)) { ability = "aotd"; label = "AOTD"; }
        else if (held.is(DatapackItems.NECRONS_SCYTHE)) { ability = "necrons_scythe"; label = "Necron's Scythe"; }
        else if (held.is(DatapackItems.ZOMBIE_SWORD)) { ability = "zombie_sword"; label = "Zombie Sword"; }
        else if (held.is(DatapackItems.ORE_TRACKER)) { ability = "ore_tracker"; label = "Ore Tracker"; }

        if (ability == null) return "";
        int remaining = mana.cooldownTicks(ability);
        return " | " + label + ": " + (remaining > 0 ? remaining + " ticks" : "READY");
    }

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

                String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                int defense = ModArmorItems.getVanillaArmorDefense(itemPath);
                int strength = ModArmorItems.getVanillaArmorStrength(itemPath);
                if (defense > 0) {
                    ItemLore existingLore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                    boolean hasDefenseLore = existingLore.lines().stream()
                            .anyMatch(line -> line.getString().startsWith("Defense:"));
                    if (!hasDefenseLore) {
                        List<Component> lines = new ArrayList<>(existingLore.lines());
                        lines.add(Component.literal("Defense: " + defense).withStyle(ChatFormatting.GREEN));
                        if (strength > 0) {
                            lines.add(Component.literal("Strength: " + strength).withStyle(ChatFormatting.RED));
                        }
                        stack.set(DataComponents.LORE, new ItemLore(lines));
                    }
                }
            }
        }
    }

    private void tick(ServerPlayer player) {
        // Keep one permanent Stat Viewer in hotbar slot 9, matching the datapack.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (slot != 8 && player.getInventory().getItem(slot).is(DatapackItems.STAT_VIEWER)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        if (!player.getInventory().getItem(8).is(DatapackItems.STAT_VIEWER)) {
            player.getInventory().setItem(8, new ItemStack(DatapackItems.STAT_VIEWER));
        }
        if (player.getOffhandItem().is(DatapackItems.STAT_VIEWER)) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        stripVanillaArmorAttributes(player);
        stats = VanillaWeaponStats.applyHeldWeapon(ModArmorItems.getSkyblockStats(player), player.getMainHandItem());

        int recalculatedMaximum = Math.max(1, stats.health() * 10);
        if (!healthInitialized) {
            maximumHealth = recalculatedMaximum;
            currentHealth = maximumHealth;
            healthInitialized = true;
        } else if (currentHealth <= 0) {
            maximumHealth = recalculatedMaximum;
            if (player.isAlive()) currentHealth = maximumHealth;
        } else {
            int healthDifference = recalculatedMaximum - maximumHealth;
            maximumHealth = recalculatedMaximum;
            currentHealth = Math.max(1, Math.min(maximumHealth, currentHealth + healthDifference));
        }

        // The custom Health pool is authoritative. Keep vanilla health as a
        // large reserve so normal damage can be measured after vanilla
        // resistance, shields, and other mitigation have run.
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        if (player.isAlive() && player.getHealth() < 1024.0F) player.setHealth(1024.0F);

        if (activePet == Pet.IRON_GOLEM && !player.hasEffect(MobEffects.RESISTANCE)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
        }
        mana.configure(maximumManaFor(activePet), manaRegenerationFor(activePet));
        mana.tick();
        renderAotvPreview(player);
        if (!manaBossBar.getPlayers().contains(player)) manaBossBar.addPlayer(player);
        manaBossBar.setName(Component.literal(
                "Mana: " + mana.currentMana() + " / " + mana.maximumMana()));
        manaBossBar.setProgress(Math.max(0.0F, Math.min(1.0F,
                mana.currentMana() / (float) mana.maximumMana())));
        updatePetDisplay(player);

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

        if (++hudUpdateTicks >= 1) {
            hudUpdateTicks = 0;
            String hud = String.format(java.util.Locale.ROOT,
                    "Health: %.1f/%.1f | Defense: %d",
                    currentHealth / 10.0, maximumHealth / 10.0, stats.defense())
                    + abilityCooldownHud(player);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(hud)));
        }
    }
}
