package dev.hypixelcustomrecipe.entity.voidridden;

import dev.hypixelcustomrecipe.HypixelCustomRecipe;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.server.level.ServerLevel;

public final class VoidriddenMobs {
    public static final EntityType<VoidriddenZombie> VOIDRIDDEN_ZOMBIE = register(
            "voidridden_zombie",
            EntityType.Builder.of(VoidriddenZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
    );
    public static final EntityType<VoidriddenSkeleton> VOIDRIDDEN_SKELETON = register(
            "voidridden_skeleton",
            EntityType.Builder.of(VoidriddenSkeleton::new, MobCategory.MONSTER).sized(0.6F, 1.99F)
    );
    public static final EntityType<VoidriddenEnderman> VOIDRIDDEN_ENDERMAN = register(
            "voidridden_enderman",
            EntityType.Builder.of(VoidriddenEnderman::new, MobCategory.MONSTER).sized(0.6F, 2.9F)
    );
    public static final EntityType<VoidriddenSpider> VOIDRIDDEN_SPIDER = register(
            "voidridden_spider",
            EntityType.Builder.of(VoidriddenSpider::new, MobCategory.MONSTER).sized(1.4F, 0.9F)
    );
    public static final EntityType<VoidriddenCreeper> VOIDRIDDEN_CREEPER = register(
            "voidridden_creeper",
            EntityType.Builder.of(VoidriddenCreeper::new, MobCategory.MONSTER).sized(0.6F, 1.7F)
    );

    private VoidriddenMobs() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(HypixelCustomRecipe.MOD_ID, name)
        );
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(
                VOIDRIDDEN_ZOMBIE, Zombie.createAttributes().add(Attributes.MAX_HEALTH, 100000.0)
        );
        FabricDefaultAttributeRegistry.register(
                VOIDRIDDEN_SKELETON, Skeleton.createAttributes().add(Attributes.MAX_HEALTH, 100000.0)
        );
        FabricDefaultAttributeRegistry.register(
                VOIDRIDDEN_ENDERMAN, EnderMan.createAttributes().add(Attributes.MAX_HEALTH, 200000.0)
        );
        FabricDefaultAttributeRegistry.register(
                VOIDRIDDEN_SPIDER, Spider.createAttributes().add(Attributes.MAX_HEALTH, 80000.0)
        );
        FabricDefaultAttributeRegistry.register(
                VOIDRIDDEN_CREEPER, Creeper.createAttributes().add(Attributes.MAX_HEALTH, 100000.0)
        );

        // Replace newly created vanilla mobs before they enter the world. Existing
        // mobs restored from world saves are deliberately left untouched.
        ServerEntityEvents.ALLOW_LOAD.register(VoidriddenMobs::replaceVanillaMob);
        HypixelCustomRecipe.LOGGER.info("Registered the five Voidridden mob types.");
    }

    private static boolean replaceVanillaMob(Entity entity, ServerLevel level,
                                             net.minecraft.world.entity.EntitySpawnReason reason,
                                             boolean isLoadedFromDisk) {
        if (isLoadedFromDisk || level.getRandom().nextFloat() >= 0.05F) {
            return true;
        }

        EntityType<?> originalType = entity.getType();
        Entity replacement = null;

        if (originalType == EntityType.ZOMBIE) {
            replacement = VOIDRIDDEN_ZOMBIE.create(level);
        } else if (originalType == EntityType.SKELETON) {
            replacement = VOIDRIDDEN_SKELETON.create(level);
        } else if (originalType == EntityType.ENDERMAN) {
            replacement = VOIDRIDDEN_ENDERMAN.create(level);
        } else if (originalType == EntityType.SPIDER) {
            replacement = VOIDRIDDEN_SPIDER.create(level);
        } else if (originalType == EntityType.CREEPER) {
            replacement = VOIDRIDDEN_CREEPER.create(level);
        }

        if (replacement == null) {
            return true;
        }

        CompoundTag data = new CompoundTag();
        entity.saveWithoutId(data);
        replacement.load(data);
        replacement.setUUID(entity.getUUID());

        // If the replacement cannot be inserted, keep the original entity instead.
        return !level.addFreshEntity(replacement);
    }
}
