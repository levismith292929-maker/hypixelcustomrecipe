package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RecipeUnlocker {
    private static final Set<ResourceKey<Recipe<?>>> MOD_RECIPE_KEYS = Stream.of(
            "holy_dragon_fragment",
            "necron_core",
            "old_dragon_fragment",
            "protector_dragon_fragment",
            "star_shard1",
            "star_shard2",
            "strong_dragon_fragment",
            "superior_dragon_fragment",
            "unstable_dragon_fragment",
            "wise_dragon_fragment",
            "young_dragon_fragment"
    ).map(path -> ResourceKey.create(
            Registries.RECIPE,
            Identifier.fromNamespaceAndPath("hypixel", path)
    )).collect(Collectors.toUnmodifiableSet());

    private RecipeUnlocker() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> MOD_RECIPE_KEYS.contains(recipe.id())
                            || (recipe.id().identifier().getNamespace().equals("hypixelrecipes")
                            && recipe.id().identifier().getPath().contains("_dragon_")))
                    .toList();

            if (!recipes.isEmpty()) {
                handler.player.awardRecipes(recipes);
            }
        });
    }
}
