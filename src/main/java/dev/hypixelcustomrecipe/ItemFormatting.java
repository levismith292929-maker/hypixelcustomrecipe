package dev.hypixelcustomrecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/** Shared Hypixel-style item name and lore formatting. */
public final class ItemFormatting {
    private ItemFormatting() {}

    public static ChatFormatting rarityColor(String path) {
        return switch (path) {
            case "necron_core", "aotv", "aotd", "astraea", "hyperion", "necrons_scythe",
                    "pet_ender_dragon", "pet_golden_dragon" -> ChatFormatting.GOLD;
            case "zombie_sword", "aote", "old_dragon_fragment", "protector_dragon_fragment",
                    "wise_dragon_fragment", "young_dragon_fragment", "strong_dragon_fragment",
                    "unstable_dragon_fragment", "superior_dragon_fragment", "pet_iron_golem",
                    "pet_enderman" -> ChatFormatting.DARK_PURPLE;
            case "ore_tracker" -> ChatFormatting.BLUE;
            default -> ChatFormatting.WHITE;
        };
    }

    public static Component name(String value, ChatFormatting rarity) {
        return Component.literal(value).withStyle(style -> style.withColor(rarity).withItalic(false));
    }

    public static Component name(String value, int rgb) {
        return Component.literal(value).withStyle(style -> style.withColor(TextColor.fromRgb(rgb)).withItalic(false));
    }

    public static Component lore(String line) {
        int click = line.indexOf("  RIGHT CLICK");
        if (line.startsWith("Ability:") && click >= 0) {
            return Component.literal(line.substring(0, click))
                    .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false))
                    .append(Component.literal(" RIGHT CLICK")
                    .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)));
        }

        int colon = line.indexOf(':');
        if (colon > 0) {
            String label = line.substring(0, colon + 1);
            String value = line.substring(colon + 1);
            ChatFormatting color = switch (label) {
                case "Health:" -> ChatFormatting.RED;
                case "Defense:" -> ChatFormatting.GREEN;
                case "Strength:", "Damage:" -> ChatFormatting.RED;
                case "Intelligence:", "Mana Cost:", "Max Mana:", "Mana Regeneration:" -> ChatFormatting.AQUA;
                case "Crit Chance:", "Crit Damage:", "Cooldown:" -> ChatFormatting.BLUE;
                case "Ability:", "Full Set Bonus:" -> ChatFormatting.GOLD;
                default -> ChatFormatting.GRAY;
            };
            ChatFormatting valueColor = color == ChatFormatting.BLUE ? ChatFormatting.GRAY : color;
            return Component.literal(label).withStyle(style -> style.withColor(color).withItalic(false))
                    .append(Component.literal(value).withStyle(style -> style.withColor(valueColor).withItalic(false)));
        }
        return Component.literal(line).withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
    }
}
