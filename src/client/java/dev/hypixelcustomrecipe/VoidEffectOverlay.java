package dev.hypixelcustomrecipe;

import dev.hypixelcustomrecipe.entity.voidridden.VoidriddenEffects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class VoidEffectOverlay {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(
            HypixelCustomRecipe.MOD_ID, "void_effect_overlay"
    );

    private VoidEffectOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ID, VoidEffectOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !client.player.hasEffect(VoidriddenEffects.VOID)) {
            return;
        }

        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xE6000000);
    }
}
