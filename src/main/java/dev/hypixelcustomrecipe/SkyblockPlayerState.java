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
import net.minecraft.core.BlockPos;
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
    private int combatTicksRemaining;
    private double healthRegenerationProgress;
    private int necronGuardCooldownTicks;
    private int skyblockLevel;
    private int levelExperience;

    private ArmorStand petDisplay;

    private SkyblockPlayerState() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) get(player).tick(player);
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            // A respawn must start with a clean effect list and a restored custom
            // Health pool. Otherwise a copied Void effect can leave the new
            // player permanently blinded/damaged, while the depleted SkyBlock
            // Health pool prevents further interaction or deaths.
            newPlayer.removeAllEffects();
            newPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
            newPlayer.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
            newPlayer.setHealth(1024.0F);
            SkyblockPlayerState state = PLAYERS.get(oldPlayer.getUUID());
            if (state != null) {
                state.resetAfterRespawn(newPlayer);
                state.removePetDisplay();
                state.manaBossBar.removePlayer(oldPlayer);
                state.manaBossBar.addPlayer(newPlayer);
            }
        });
        // Run the same cleanup after the new player has actually been installed
        // by the server's respawn flow, not only while copying the old player.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            newPlayer.removeAllEffects();
            SkyblockPlayerState state = PLAYERS.get(newPlayer.getUUID());
            if (state != null) {
                state.resetAfterRespawn(newPlayer);
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

    private void resetAfterRespawn(ServerPlayer player) {
        if (maximumHealth <= 0) {
            maximumHealth = SkyblockStats.BASE.health();
        }
        currentHealth = maximumHealth;
        healthInitialized = true;
        combatTicksRemaining = 0;
        healthRegenerationProgress = 0.0;
        player.setHealth(1024.0F);
    }

    public static SkyblockPlayerState get(Player player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new SkyblockPlayerState());
    }

    public SkyblockStats stats() { return stats; }
    public SkyblockManaState mana() { return mana; }
    public Pet activePet() { return activePet; }
    public int currentHealth() { return currentHealth; }
    public int maximumHealth() { return maximumHealth; }
    public int level() { return skyblockLevel; }
    public int levelExperience() { return levelExperience; }
    public int levelExperienceRequired() { return requiredExperienceForLevel(skyblockLevel); }

    /** Adds gained Minecraft XP levels to the separate SkyBlock level progression. */
    public void addLevelExperience(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        levelExperience = Math.addExact(levelExperience, amount);
        while (levelExperience >= levelExperienceRequired()) {
            levelExperience -= levelExperienceRequired();
            skyblockLevel++;
            player.sendSystemMessage(Component.literal(
                    "SkyBlock Level Up! You are now Level " + skyblockLevel + "!")
                    .withStyle(style -> style.withColor(0x55FFFF).withBold(true)));
        }
    }

    /** Five vanilla Minecraft XP levels are required for SkyBlock Level 1. */
    private static int requiredExperienceForLevel(int currentLevel) {
        long cost = 5L;
        for (int i = 0; i < currentLevel; i++) {
            cost = (long) Math.ceil(cost * 1.65D);
            if (cost >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) cost;
    }

    /** Subtracts post-vanilla-mitigation damage using the custom Defense formula. */
    public boolean applyIncomingDamage(float vanillaDamage) {
        int effective = (int) Math.round(stats.applyDefense(vanillaDamage) * 10.0);
        if (effective > 0) currentHealth = Math.max(0, currentHealth - effective);
        return currentHealth <= 0;
    }

    /** Keeps Health regeneration paused for five seconds after entity combat. */
    public void markCombat() {
        combatTicksRemaining = 100;
        healthRegenerationProgress = 0.0;
    }

    /** Restores custom Health points; one vanilla heart is 20 custom points. */
    public void heal(int amount) {
        if (amount > 0) currentHealth = Math.min(maximumHealth, currentHealth + amount);
    }

    /** Starts the Necron guard set-bonus cooldown without charging mana. */
    public boolean tryStartNecronGuardCooldown() {
        if (necronGuardCooldownTicks > 0) return false;
        necronGuardCooldownTicks = 600;
        return true;
    }


    public void savePersistentData(ServerPlayer player, ValueOutput output) {
        output.putInt("hypixel_health", currentHealth);
        output.putInt("hypixel_max_health", maximumHealth);
        output.putInt("hypixel_pet", activePet.ordinal());
        output.putInt("hypixel_necron_guard_cooldown", necronGuardCooldownTicks);
        output.putInt("hypixel_skyblock_level", skyblockLevel);
        output.putInt("hypixel_skyblock_level_xp", levelExperience);
        mana.savePersistentData(output);
        AccessoryBag.save(player, output);
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
        AccessoryBag.load(player, input);
        necronGuardCooldownTicks = Math.max(0, input.getIntOr("hypixel_necron_guard_cooldown", 0));
        skyblockLevel = Math.max(0, input.getIntOr("hypixel_skyblock_level", 0));
        levelExperience = Math.max(0, input.getIntOr("hypixel_skyblock_level_xp", 0));
        levelExperience = Math.min(levelExperience, levelExperienceRequired() - 1);
        int petIndex = input.getIntOr("hypixel_pet", 0);
        Pet[] pets = Pet.values();
        if (petIndex >= 0 && petIndex < pets.length) {
            activePet = pets[petIndex];

        }
    }

    public void startAstraeaOrbs() { astraeaOrbTicks = 100; }


    private int maximumManaFor(Pet pet) {
        int intelligenceBonus = stats.manaMaximum() - SkyblockStats.BASE.manaMaximum();
        return Math.max(1, pet.manaMaximum() + intelligenceBonus);
    }

    private int manaRegenerationFor(Pet pet) {
        int levelScaledBase = SkyblockLevelBonuses.scaleGeneral(SkyblockStats.BASE.manaRegeneration(), skyblockLevel);
        int petRegeneration = SkyblockLevelBonuses.scaleGeneral(pet.manaRegeneration(), skyblockLevel);
        int intelligenceBonus = stats.manaRegeneration() - levelScaledBase;
        return Math.max(0, petRegeneration + intelligenceBonus);
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
        petDisplay.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS,
                (byte) (ArmorStand.CLIENT_FLAG_SMALL
                        | ArmorStand.CLIENT_FLAG_NO_BASEPLATE
                        | ArmorStand.CLIENT_FLAG_MARKER));
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


    /** Shows the end-rod preview using the same 0.1-block raycast as AOTV. */
    private static void renderAotvPreview(ServerPlayer player) {
        if (!player.getMainHandItem().is(DatapackItems.AOTV)) return;
        BlockPos pos = SkyblockAbilityHandler.findRaycastTargetForPreview(player);
        if (pos != null) {
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

    private static boolean isArmorPiece(ServerPlayer player, EquipmentSlot slot, String itemId) {
        String path = BuiltInRegistries.ITEM.getKey(player.getItemBySlot(slot).getItem()).getPath();
        return path.equals(itemId) || (path.startsWith(itemId + "_")
                && path.substring((itemId + "_").length()).matches("[0-9]+star"));
    }

    private void tickHealthRegeneration(ServerPlayer player) {
        if (combatTicksRemaining > 0) {
            combatTicksRemaining--;
            healthRegenerationProgress = 0.0;
            return;
        }

        if (currentHealth >= maximumHealth) {
            healthRegenerationProgress = 0.0;
            return;
        }

        // Each armor piece contributes its own regeneration bonus per 4 seconds.
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] suffixes = {"_helmet", "_chestplate", "_leggings", "_boots"};
        double armorBonusPerFourSeconds = 0.0;

        // Protector Dragon piece values: helmet 15, chestplate 30,
        // leggings 20, boots 10 Health per 4 seconds (75 total).
        if (isArmorPiece(player, EquipmentSlot.HEAD, "protector_dragon_helmet")) armorBonusPerFourSeconds += 15.0;
        if (isArmorPiece(player, EquipmentSlot.CHEST, "protector_dragon_chestplate")) armorBonusPerFourSeconds += 30.0;
        if (isArmorPiece(player, EquipmentSlot.LEGS, "protector_dragon_leggings")) armorBonusPerFourSeconds += 20.0;
        if (isArmorPiece(player, EquipmentSlot.FEET, "protector_dragon_boots")) armorBonusPerFourSeconds += 10.0;

        // Necron's individual piece values: helmet 70, chestplate 180,
        // leggings 100, boots 25 Health per 4 seconds.
        if (isArmorPiece(player, EquipmentSlot.HEAD, "necron_helmet")) armorBonusPerFourSeconds += 70.0;
        if (isArmorPiece(player, EquipmentSlot.CHEST, "necron_chestplate")) armorBonusPerFourSeconds += 180.0;
        if (isArmorPiece(player, EquipmentSlot.LEGS, "necron_leggings")) armorBonusPerFourSeconds += 100.0;
        if (isArmorPiece(player, EquipmentSlot.FEET, "necron_boots")) armorBonusPerFourSeconds += 25.0;

        double healthPerFourSeconds = 25.0 + armorBonusPerFourSeconds;
        healthPerFourSeconds = Math.ceil(healthPerFourSeconds * SkyblockLevelBonuses.vitalityMultiplier(skyblockLevel));
        healthRegenerationProgress += healthPerFourSeconds / 80.0;

        int wholeHealth = (int) healthRegenerationProgress;
        if (wholeHealth > 0) {
            heal(wholeHealth);
            healthRegenerationProgress -= wholeHealth;
        }
    }

    private void tick(ServerPlayer player) {
        if (necronGuardCooldownTicks > 0) necronGuardCooldownTicks--;
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
        stats = AccessoryBag.applyAdminRing(player, SkyblockLevelBonuses.apply(
                VanillaWeaponStats.applyHeldWeapon(ModArmorItems.getSkyblockStats(player), player.getMainHandItem()),
                skyblockLevel));

        int recalculatedMaximum = Math.max(1, stats.health());
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
        tickHealthRegeneration(player);
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
            double rotation = (100 - astraeaOrbTicks) * Math.toRadians(5.0);
            var darkDust = new net.minecraft.core.particles.DustParticleOptions(0x141414, 2.0F);
            for (int i = 0; i < 4; i++) {
                double angle = rotation + i * (Math.PI / 2.0);
                double x = player.getX() + Math.cos(angle) * 2.2;
                double y = player.getY() + 1.0;
                double z = player.getZ() + Math.sin(angle) * 2.2;
                level.sendParticles(darkDust, x, y, z, 5, 0.06, 0.06, 0.06, 0.0);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        x, y + 0.2, z, 1, 0.15, 0.15, 0.15, 0.0);
                level.sendParticles(darkDust, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            astraeaOrbTicks--;
        }

        if (++hudUpdateTicks >= 1) {
            hudUpdateTicks = 0;
            String hud = String.format(java.util.Locale.ROOT,
                    "Health: %d/%d | Defense: %d | Level: %d (%d/%d XP)",
                    (int) Math.round(currentHealth / 10.0), (int) Math.round(maximumHealth / 10.0), stats.defense(),
                    skyblockLevel, levelExperience, levelExperienceRequired())
                    + abilityCooldownHud(player);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(hud)));
        }
    }
}
