package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class RecipeUnlocker {
    private RecipeUnlocker() {
    }

    public static void initialize() {
        // Wait until the player has fully joined and the server recipe manager is ready.
        // Award the complete mod recipe set so it is available in the recipe book for autofill.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            if (handler.player.isRemoved()) {
                return;
            }

            var recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> recipe.id().identifier().getNamespace().equals("hypixel")
                            || recipe.id().identifier().getNamespace().equals("hypixelrecipes"))
                    .toList();

            if (!recipes.isEmpty()) {
                handler.player.awardRecipes(recipes);
            }
        }));
    }
}
