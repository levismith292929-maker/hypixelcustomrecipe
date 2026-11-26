package dev.hypixelcustomrecipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.player.Player;

/**
 * Vanilla experience remains entirely vanilla for enchanting.
 * SkyBlock Player XP is granted only by Combat and Mining category level-ups.
 */
@Mixin(Player.class)
public abstract class PlayerExperienceMixin {
}
