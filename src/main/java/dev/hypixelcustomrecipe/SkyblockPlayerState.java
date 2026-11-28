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
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

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
        GOLDEN_DRAGON(750, 10, 0.40, 0),
        IRON_GOLEM(600, 30, 0.05, 0),
        MAXOR(500, 40, 0.0, 10),
        STORM(500, 40, 0.0, 5),
        GOLDOR(500, 40, 0.0, 5),
        NECRON(500, 40, 0.50, 25);

        private final int manaMaximum;
        private final int manaRegeneration;
        private final double damageBonus;
        private final int trueDefenseBonus;

        Pet(int manaMaximum, int manaRegeneration, double damageBonus) {
            this(manaMaximum, manaRegeneration, damageBonus, 0);
        }

        Pet(int manaMaximum, int manaRegeneration, double damageBonus, int trueDefenseBonus) {
            this.manaMaximum = manaMaximum;
            this.manaRegeneration = manaRegeneration;
            this.damageBonus = damageBonus;
            this.trueDefenseBonus = trueDefenseBonus;
        }

        public int manaMaximum() { return manaMaximum; }
        public int manaRegeneration() { return manaRegeneration; }
        public double damageMultiplier() { return 1.0 + damageBonus; }
        public int trueDefenseBonus() { return trueDefenseBonus; }
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
    private int maxorLaserStacks;
    private int necronCorruptDamage;
    private int necronFlameWallStacks;
    private boolean healthInitialized;
    private int combatTicksRemaining;
    private double healthRegenerationProgress;
    private int necronGuardCooldownTicks;
    private int skyblockLevel;
    private int levelExperience;
    private int combatLevel;
    private int combatExperience;
    private int miningLevel;
    private int miningExperience;
    private boolean levelMessagesEnabled = true;
    private String recentExperienceActionBar = "";
    private int recentExperienceActionBarTicks;

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
            newPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000000.0);
            newPlayer.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
            newPlayer.setHealth(1000000000.0F);
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
                if (state.activePet == Pet.STORM) {
                    boolean hasSoulBlade = false;
                    for (int slot = 0; slot < newPlayer.getInventory().getContainerSize(); slot++) {
                        if (newPlayer.getInventory().getItem(slot).is(DatapackItems.SOUL_BLADE)) {
                            hasSoulBlade = true;
                            break;
                        }
                    }
                    if (newPlayer.getOffhandItem().is(DatapackItems.SOUL_BLADE)) hasSoulBlade = true;
                    if (!hasSoulBlade) {
                        ItemStack soulBlade = new ItemStack(DatapackItems.SOUL_BLADE);
                        if (!newPlayer.getInventory().add(soulBlade)) newPlayer.drop(soulBlade, false);
                    }
                }
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
        maxorLaserStacks = 0;
        necronCorruptDamage = 0;
        necronFlameWallStacks = 0;
        if (maximumHealth <= 0) {
            maximumHealth = SkyblockStats.BASE.health();
        }
        currentHealth = maximumHealth;
        healthInitialized = true;
        combatTicksRemaining = 0;
        healthRegenerationProgress = 0.0;
        player.setHealth(1000000000.0F);
    }

    public static SkyblockPlayerState get(Player player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new SkyblockPlayerState());
    }

    public SkyblockStats stats() { return stats; }
    public SkyblockManaState mana() { return mana; }
    public Pet activePet() { return activePet; }
    public int currentHealth() { return currentHealth; }
    public int maximumHealth() { return maximumHealth; }
    public void addMaxorLaserStack() { maxorLaserStacks++; }
    public void clearMaxorLaserStacks() { maxorLaserStacks = 0; }
    /** Corrupt damage permanently lowers the encounter's healing ceiling until reset. */
    public boolean applyNecronCorruptDamage(float damage) {
        int effective = (int) Math.round(Math.max(0.0F, damage));
        if (effective > 0) {
            necronCorruptDamage = (int) Math.min(Integer.MAX_VALUE,
                    (long) necronCorruptDamage + effective);
            currentHealth = Math.max(0, currentHealth - effective);
        }
        return currentHealth <= 0;
    }
    /** One flame-wall hit permanently lowers maximum Health by ten percent for this fight. */
    public void addNecronFlameWallStack() { necronFlameWallStacks++; }
    public int necronHealingCap() { return Math.max(0, maximumHealth - necronCorruptDamage); }
    public void clearNecronEncounterEffects() {
        necronCorruptDamage = 0;
        necronFlameWallStacks = 0;
    }
    public static void clearNecronEncounterEffectsForAllPlayers() {
        for (SkyblockPlayerState state : PLAYERS.values()) state.clearNecronEncounterEffects();
    }
    public static void clearMaxorLaserStacksFor(UUID playerId) {
        SkyblockPlayerState state = PLAYERS.get(playerId);
        if (state != null) state.clearMaxorLaserStacks();
    }
    /** Necron's opening nuke wipes the player's progression and stored accessories. */
    public void applyNecronNuke(ServerPlayer player) {
        skyblockLevel = 0;
        levelExperience = 0;
        combatLevel = 0;
        combatExperience = 0;
        miningLevel = 0;
        miningExperience = 0;
        maxorLaserStacks = 0;
        AccessoryBag.clear(player);
        GemTankMenu.clear(player);
        player.getInventory().clearContent();
        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0.0F;
        currentHealth = 0;
        player.setHealth(0.0F);
        player.die(player.damageSources().magic());
    }

    public int level() { return skyblockLevel; }
    public int levelExperience() { return levelExperience; }
    public int levelExperienceRequired() { return 50; }
    public int combatLevel() { return combatLevel; }
    public int combatExperience() { return combatExperience; }
    public int combatExperienceRequired() { return categoryExperienceRequired(100, combatLevel); }
    public int miningLevel() { return miningLevel; }
    public int miningExperience() { return miningExperience; }
    public int miningExperienceRequired() { return categoryExperienceRequired(250, miningLevel); }
    public double categoryExperienceMultiplier() { return 1.0D + 0.10D * skyblockLevel; }
    public boolean levelMessagesEnabled() { return levelMessagesEnabled; }
    public void toggleLevelMessages() { levelMessagesEnabled = !levelMessagesEnabled; }

    private static int categoryExperienceRequired(int base, int level) {
        double cost = base;
        for (int i = 0; i < level; i++) {
            cost = Math.ceil(cost * 1.08D);
            if (cost >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) cost;
    }

    public void addCombatExperience(ServerPlayer player, int baseAmount) {
        addCategoryExperience(player, baseAmount, true);
    }

    public void addMiningExperience(ServerPlayer player, int baseAmount) {
        addCategoryExperience(player, baseAmount, false);
    }

    private void addCategoryExperience(ServerPlayer player, int baseAmount, boolean combat) {
        if (baseAmount <= 0) return;
        int gained = Math.max(1, (int) Math.floor(baseAmount * categoryExperienceMultiplier()));
        hypixelcustomrecipe$playExperienceSound(player);
        if (combat) {
            combatExperience = Math.addExact(combatExperience, gained);
            while (combatExperience >= combatExperienceRequired()) {
                combatExperience -= combatExperienceRequired();
                combatLevel++;
                categoryLevelUp(player, "Combat", combatLevel);
            }
        } else {
            miningExperience = Math.addExact(miningExperience, gained);
            while (miningExperience >= miningExperienceRequired()) {
                miningExperience -= miningExperienceRequired();
                miningLevel++;
                categoryLevelUp(player, "Mining", miningLevel);
            }
        }
        recentExperienceActionBar = "+" + gained + (combat ? " Combat XP" : " Mining XP");
        recentExperienceActionBarTicks = 40;
    }

    private void categoryLevelUp(ServerPlayer player, String category, int level) {
        addLevelExperience(player, 5);
        if (levelMessagesEnabled) {
            player.sendSystemMessage(Component.literal(category + " Level Up! Level " + level
                    + " (+5 Player XP, +5 Base HP, +1 Base Defense)")
                    .withStyle(style -> style.withColor(0xFFAA00).withBold(true)));
        }
    }

    private static void hypixelcustomrecipe$playExperienceSound(ServerPlayer player) {
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EXPERIENCE_ORB_PICKUP),
                SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                0.25F, 1.5F, player.getRandom().nextLong()
        ));
    }

    /** Adds gained Minecraft XP levels to the separate SkyBlock level progression. */
    public void addLevelExperience(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        hypixelcustomrecipe$playExperienceSound(player);
        levelExperience = Math.addExact(levelExperience, amount);
        while (levelExperience >= 50) {
            levelExperience -= 50;
            skyblockLevel++;
            if (levelMessagesEnabled) {
                player.sendSystemMessage(Component.literal(
                        "SkyBlock Level Up! You are now Level " + skyblockLevel + "!")
                        .withStyle(style -> style.withColor(0x55FFFF).withBold(true)));
            }
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
    public double applyDefense(double incomingDamage) {
        return stats.applyDefense(incomingDamage, activePet.trueDefenseBonus());
    }

    public boolean applyIncomingDamage(float vanillaDamage) {
        int effective = (int) Math.round(applyDefense(vanillaDamage) * 10.0);
        if (effective > 0) currentHealth = Math.max(0, currentHealth - effective);
        return currentHealth <= 0;
    }

    /** Applies fixed percentage damage without the Defense reduction. */
    public boolean applyTrueDamage(float damage) {
        int effective = (int) Math.round(Math.max(0.0F, damage) * 10.0);
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
        // Zero custom Health is a committed death state. Only the respawn
        // callbacks may restore Health; regeneration and healing cannot revive
        // a player while the server is processing death.
        if (amount > 0 && currentHealth > 0) {
            currentHealth = Math.min(necronHealingCap(), currentHealth + amount);
        }
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
        output.putInt("hypixel_combat_level", combatLevel);
        output.putInt("hypixel_combat_xp", combatExperience);
        output.putInt("hypixel_mining_level", miningLevel);
        output.putInt("hypixel_mining_xp", miningExperience);
        output.putBoolean("hypixel_level_messages_enabled", levelMessagesEnabled);
        mana.savePersistentData(output);
        AccessoryBag.save(player, output);
        GemTankMenu.save(player, output);
    }

    public void loadPersistentData(ServerPlayer player, ValueInput input) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000000.0);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        if (player.isAlive()) player.setHealth(1000000000.0F);

        int savedMaximum = input.getIntOr("hypixel_max_health", 0);
        int savedHealth = input.getIntOr("hypixel_health", -1);
        if (savedMaximum > 0 && savedHealth >= 0) {
            maximumHealth = savedMaximum;
            // A zero-Health value can be saved if the server stops while a
            // player is in the death transition. A newly loaded player is
            // alive again, so do not restore that stale dead state on login.
            currentHealth = savedHealth == 0 ? savedMaximum : Math.min(savedHealth, savedMaximum);
            healthInitialized = true;
        }

        mana.loadPersistentData(input);
        AccessoryBag.load(player, input);
        GemTankMenu.load(player, input);
        necronGuardCooldownTicks = Math.max(0, input.getIntOr("hypixel_necron_guard_cooldown", 0));
        skyblockLevel = Math.max(0, input.getIntOr("hypixel_skyblock_level", 0));
        levelExperience = Math.max(0, input.getIntOr("hypixel_skyblock_level_xp", 0));
        levelExperience = Math.min(levelExperience, 49);
        combatLevel = Math.max(0, input.getIntOr("hypixel_combat_level", 0));
        combatExperience = Math.max(0, input.getIntOr("hypixel_combat_xp", 0));
        combatExperience = Math.min(combatExperience, combatExperienceRequired() - 1);
        miningLevel = Math.max(0, input.getIntOr("hypixel_mining_level", 0));
        miningExperience = Math.max(0, input.getIntOr("hypixel_mining_xp", 0));
        miningExperience = Math.min(miningExperience, miningExperienceRequired() - 1);
        levelMessagesEnabled = input.getBooleanOr("hypixel_level_messages_enabled", true);
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
        int intelligenceBonus = stats.manaRegeneration() - SkyblockStats.BASE.manaRegeneration();
        return Math.max(0, pet.manaRegeneration() + intelligenceBonus);
    }

    public SkyblockStats applyPetStatBonuses(SkyblockStats base) {
        return switch (activePet) {
            case MAXOR -> new SkyblockStats(base.health(), base.defense() * 2,
                    base.strength(), base.armorStrength(), base.weaponStrength(),
                    base.critChance(), base.critDamage(), base.intelligence(),
                    base.manaMaximum(), base.manaRegeneration(), base.weaponDamage());
            case GOLDOR -> new SkyblockStats(base.health(), base.defense(),
                    base.strength() * 2, base.armorStrength() * 2, base.weaponStrength() * 2,
                    base.critChance(), base.critDamage(), Math.max(0, (int) Math.round(base.intelligence() * 0.90D)),
                    base.manaMaximum(), base.manaRegeneration(), base.weaponDamage());
            case NECRON -> new SkyblockStats(base.health() * 2, base.defense() * 2,
                    base.strength() * 2, base.armorStrength() * 2, base.weaponStrength() * 2,
                    base.critChance() * 2, base.critDamage() * 2, base.intelligence() * 2,
                    base.manaMaximum() * 2, base.manaRegeneration() * 2, base.weaponDamage() * 2.0D);
            default -> base;
        };
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
            case MAXOR -> head = new ItemStack(Items.WITHER_SKELETON_SKULL);
            case STORM -> head = new ItemStack(Items.SKELETON_SKULL);
            case GOLDOR -> head = new ItemStack(Items.ZOMBIE_HEAD);
            case NECRON -> head = new ItemStack(Items.WITHER_SKELETON_SKULL);
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
                && path.substring((itemId + "_").length()).matches("(?:[0-9]+star|3star_void)"));
    }

    private void tickHealthRegeneration(ServerPlayer player) {
        if (currentHealth <= 0 || !player.isAlive()) {
            healthRegenerationProgress = 0.0;
            return;
        }
        if (combatTicksRemaining > 0) {
            combatTicksRemaining--;
            healthRegenerationProgress = 0.0;
            return;
        }

        if (currentHealth >= necronHealingCap()) {
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
        
        healthRegenerationProgress += healthPerFourSeconds / 80.0;

        int wholeHealth = (int) healthRegenerationProgress;
        if (wholeHealth > 0) {
            heal(wholeHealth);
            healthRegenerationProgress -= wholeHealth;
        }
    }

    public SkyblockStats applyCategoryBonuses(SkyblockStats base) {
        int totalCategoryLevels = combatLevel + miningLevel;
        return base.add(totalCategoryLevels * 50, totalCategoryLevels, 0, 0, 0, 0, 0, 0, 0);
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
        stats = applyPetStatBonuses(SkyblockLevelBonuses.apply(AccessoryBag.applyGemstoneAccessories(player, AccessoryBag.applyAdminRing(player, applyCategoryBonuses(
                VanillaWeaponStats.applyHeldWeapon(ModArmorItems.getSkyblockStats(player), player.getMainHandItem())))), skyblockLevel));

        int recalculatedMaximum = Math.max(1, (int) Math.floor(stats.health()
                * Math.pow(0.98D, maxorLaserStacks) * Math.pow(0.90D, necronFlameWallStacks)));
        if (!healthInitialized) {
            maximumHealth = recalculatedMaximum;
            currentHealth = maximumHealth;
            healthInitialized = true;
        } else if (currentHealth <= 0) {
            // Do not refill the external Health pool here. A player at zero
            // must remain dead until ServerPlayerEvents.AFTER_RESPAWN calls
            // resetAfterRespawn().
            maximumHealth = recalculatedMaximum;
        } else {
            int healthDifference = recalculatedMaximum - maximumHealth;
            maximumHealth = recalculatedMaximum;
            currentHealth = Math.max(1, Math.min(necronHealingCap(), currentHealth + healthDifference));
        }

        // The custom Health pool is authoritative. Keep vanilla health as a
        // very large reserve so even large custom hits cannot trigger vanilla death before
        // resistance, shields, and other mitigation have run.
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000000.0);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        if (player.isAlive() && player.getHealth() < 1000000000.0F) player.setHealth(1000000000.0F);

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

        if (recentExperienceActionBarTicks > 0) {
            recentExperienceActionBarTicks--;
        } else {
            recentExperienceActionBar = "";
        }

        if (++hudUpdateTicks >= 1) {
            hudUpdateTicks = 0;
            var hud = Component.literal("Health: ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(String.format(java.util.Locale.ROOT, "%d/%d",
                            (int) Math.round(currentHealth / 10.0),
                            (int) Math.round(maximumHealth / 10.0)))
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal(" | ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("Defense: ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(Integer.toString(stats.defense()))
                            .withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(String.format(java.util.Locale.ROOT,
                            " | Level: %d (%d/%d XP)", skyblockLevel, levelExperience,
                            levelExperienceRequired())).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(abilityCooldownHud(player)).withStyle(ChatFormatting.WHITE));
            if (!recentExperienceActionBar.isEmpty()) {
                hud = hud.append(Component.literal(" | " + recentExperienceActionBar)
                        .withStyle(ChatFormatting.YELLOW));
            }
            player.connection.send(new ClientboundSetActionBarTextPacket(hud));
        }
    }
}
