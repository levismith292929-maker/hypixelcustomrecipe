package dev.hypixelcustomrecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Automatic Necron full-set guard summon. */
public final class NecronGuardHandler {
    private static final Map<UUID, UUID> GUARD_OWNERS = new ConcurrentHashMap<>();

    private NecronGuardHandler() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                for (Map.Entry<UUID, UUID> entry : GUARD_OWNERS.entrySet()) {
                    if (!(level.getEntity(entry.getKey()) instanceof WitherSkeleton guard)) continue;
                    if (!(level.getEntity(entry.getValue()) instanceof ServerPlayer owner)) continue;
                    LivingEntity attacker = owner.getLastHurtByMob();
                    if (attacker != null && attacker.isAlive() && attacker != owner
                            && attacker.level() == level && guard.canAttack(attacker)) {
                        guard.setTarget(attacker);
                    }
                }
            }
        });
    }

    public static boolean isGuardOwner(Mob guard, LivingEntity target) {
        UUID owner = GUARD_OWNERS.get(guard.getUUID());
        return owner != null && owner.equals(target.getUUID());
    }

    public static void onPlayerHit(ServerPlayer player) {
        if (!ModArmorItems.isWearingFullNecronSet(player)) return;
        SkyblockPlayerState state = SkyblockPlayerState.get(player);
        if (!state.tryStartNecronGuardCooldown()) return;
        summonGuards(player);
    }

    private static void summonGuards(ServerPlayer owner) {
        ServerLevel level = (ServerLevel) owner.level();
        Player target = level.players().stream()
                .filter(candidate -> candidate != owner
                        && !candidate.getGameProfile().name().equalsIgnoreCase("QWDad"))
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);

        Vec3 side = owner.getLookAngle().cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1.0, 0.0, 0.0);
        side = side.normalize();

        for (int i = 0; i < 2; i++) {
            var witherSkeletonType = BuiltInRegistries.ENTITY_TYPE.get(
                    Identifier.fromNamespaceAndPath("minecraft", "wither_skeleton")).orElseThrow().value();
            var spawned = witherSkeletonType.create(level, EntitySpawnReason.TRIGGERED);
            if (!(spawned instanceof WitherSkeleton guard)) continue;

            double direction = i == 0 ? -1.0 : 1.0;
            guard.setPos(owner.getX() + side.x * 1.25 * direction, owner.getY(),
                    owner.getZ() + side.z * 1.25 * direction);
            guard.setYRot(owner.getYRot());
            guard.setXRot(0.0F);
            guard.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
            guard.setHealth(40.0F);
            guard.setPersistenceRequired();
            guard.setCanPickUpLoot(false);
            
            guard.setCustomName(net.minecraft.network.chat.Component.literal("Necron's Guard"));

            ItemStack helmet = enchantedNetherite(level, Items.NETHERITE_HELMET);
            ItemStack chestplate = enchantedNetherite(level, Items.NETHERITE_CHESTPLATE);
            ItemStack leggings = enchantedNetherite(level, Items.NETHERITE_LEGGINGS);
            ItemStack boots = enchantedNetherite(level, Items.NETHERITE_BOOTS);
            guard.setItemSlot(EquipmentSlot.HEAD, helmet);
            guard.setItemSlot(EquipmentSlot.CHEST, chestplate);
            guard.setItemSlot(EquipmentSlot.LEGS, leggings);
            guard.setItemSlot(EquipmentSlot.FEET, boots);
            guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.MAINHAND
            }) {
                guard.setDropChance(slot, 0.0F);
            }
            // Start by targeting the nearest player except QWDad.
            if (target != null) guard.setTarget(target);
            if (level.addFreshEntity(guard)) {
                GUARD_OWNERS.put(guard.getUUID(), owner.getUUID());
            }
        }
    }

    private static ItemStack enchantedNetherite(ServerLevel level, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 5);
        return stack;
    }
}
