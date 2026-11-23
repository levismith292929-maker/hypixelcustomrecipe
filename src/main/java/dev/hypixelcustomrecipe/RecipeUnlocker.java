package dev.hypixelcustomrecipe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public final class RecipeUnlocker {
    private RecipeUnlocker() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            List<RecipeHolder<?>> recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> recipe.id().getNamespace().equals("hypixel"))
                    .toList();

            if (!recipes.isEmpty()) {
                handler.player.awardRecipes(recipes);
            }
        });
    }
}
