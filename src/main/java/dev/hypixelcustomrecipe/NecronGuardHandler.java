package dev.hypixelcustomrecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Automatic Necron full-set guard summon. */
public final class NecronGuardHandler {
    private NecronGuardHandler() {}

    public static void onPlayerHit(ServerPlayer player) {
        if (!ModArmorItems.isWearingFullNecronSet(player)) return;
        SkyblockPlayerState state = SkyblockPlayerState.get(player);
        if (!state.tryStartNecronGuardCooldown()) return;
        summonGuards(player);
    }

    private static void summonGuards(ServerPlayer owner) {
        ServerLevel level = (ServerLevel) owner.level();
        Player target = level.players().stream()
                .filter(candidate -> !candidate.getGameProfile().name().equalsIgnoreCase("QWDad"))
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);

        Vec3 side = owner.getLookAngle().cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1.0, 0.0, 0.0);
        side = side.normalize();

        for (int i = 0; i < 2; i++) {
            WitherSkeleton guard = EntityType.WITHER_SKELETON.create(level, EntitySpawnReason.TRIGGERED);
            if (guard == null) continue;

            double direction = i == 0 ? -1.0 : 1.0;
            guard.moveTo(owner.getX() + side.x * 1.25 * direction, owner.getY(),
                    owner.getZ() + side.z * 1.25 * direction, owner.getYRot(), 0.0F);
            guard.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
            guard.setHealth(40.0F);
            guard.setPersistenceRequired();
            guard.setCanPickUpLoot(false);
            guard.addTag("hypixel_necron_guard");
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
            guard.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?>);
            guard.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                    guard, Player.class, 10, true, false,
                    (candidate, serverLevel) -> !(candidate instanceof ServerPlayer serverPlayer)
                            || !serverPlayer.getGameProfile().name().equalsIgnoreCase("QWDad")));
            if (target != null) guard.setTarget(target);
            level.addFreshEntity(guard);
        }
    }

    private static ItemStack enchantedNetherite(ServerLevel level, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 5);
        return stack;
    }
}
