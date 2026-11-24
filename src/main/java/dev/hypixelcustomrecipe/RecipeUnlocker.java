package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class RecipeUnlocker {
    private RecipeUnlocker() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> recipe.id().identifier().getNamespace().equals("hypixel")
                            || (recipe.id().identifier().getNamespace().equals("hypixelrecipes")
                            && recipe.id().identifier().getPath().contains("_dragon_")))
                    .toList();

            if (!recipes.isEmpty()) {
                handler.player.awardRecipes(recipes);
            }
        });
    }
}
