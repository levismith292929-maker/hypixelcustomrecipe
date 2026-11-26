package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.Set;

public final class RecipeUnlocker {
    private static final Set<String> MODDED_MINECRAFT_RECIPE_IDS = Set.of(
            "copper_axe",
            "copper_boots",
            "copper_chestplate",
            "copper_helmet",
            "copper_leggings",
            "copper_sword",
            "diamond_axe",
            "diamond_boots",
            "diamond_chestplate",
            "diamond_helmet",
            "diamond_leggings",
            "diamond_sword",
            "golden_axe",
            "golden_boots",
            "golden_chestplate",
            "golden_helmet",
            "golden_leggings",
            "golden_sword",
            "iron_axe",
            "iron_boots",
            "iron_chestplate",
            "iron_helmet",
            "iron_leggings",
            "iron_sword",
            "leather_boots",
            "leather_chestplate",
            "leather_helmet",
            "leather_leggings",
            "netherite_axe_smithing",
            "netherite_boots_smithing",
            "netherite_chestplate_smithing",
            "netherite_helmet_smithing",
            "netherite_leggings_smithing",
            "netherite_sword_smithing",
            "stone_axe",
            "stone_sword",
            "wooden_axe",
            "wooden_sword"
    );

    private RecipeUnlocker() {
    }

    public static void initialize() {
        // Vanilla only sends recipe-book data by default. Synchronize every serializer
        // used by this mod's recipes so clients can display and autofill them.
        RecipeSynchronization.synchronizeRecipeSerializer(
                BuiltInRegistries.RECIPE_SERIALIZER.get(
                        Identifier.fromNamespaceAndPath("minecraft", "crafting_shaped")
                )
        );
        RecipeSynchronization.synchronizeRecipeSerializer(
                BuiltInRegistries.RECIPE_SERIALIZER.get(
                        Identifier.fromNamespaceAndPath("minecraft", "smithing_transform")
                )
        );

        // Give every mod recipe to players when they join, making it available in the
        // recipe book immediately. Include the custom vanilla-namespace recipes without
        // unlocking the entire vanilla recipe book.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            if (handler.player.isRemoved()) {
                return;
            }

            var recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> {
                        var id = recipe.id().identifier();
                        String namespace = id.getNamespace();
                        return namespace.equals("hypixel")
                                || namespace.equals("hypixelrecipes")
                                || (namespace.equals("minecraft")
                                && MODDED_MINECRAFT_RECIPE_IDS.contains(id.getPath()));
                    })
                    .toList();

            if (!recipes.isEmpty()) {
                handler.player.awardRecipes(recipes);
            }
        }));
    }
}
