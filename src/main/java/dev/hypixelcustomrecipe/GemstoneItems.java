package dev.hypixelcustomrecipe;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The 40 Hypixel gemstone heads (eight gemstones, five qualities).
 * Texture hashes are from the matching Minecraft-Heads entries.
 */
public final class GemstoneItems {
    private static final String NAMESPACE = "hypixel";

    public static final Item ROUGH_RUBY_GEMSTONE = head("rough_ruby_gemstone", "Rough Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDE1OWIwMzI0M2JlMThhMTRmM2VhZTc2M2M0NTY1Yzc4ZjFmMzM5YTg3NDJkMjZmZGU1NDFiZTU5YjdkZTA3In19fQ==");
    public static final Item FLAWED_RUBY_GEMSTONE = head("flawed_ruby_gemstone", "Flawed Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDZkODEwNjhjYmRmNGEzNjQyMzFhMjY0NTNkNmNkNjYwYTAwOTVmOWNkODc5NTMwN2M1YmU2Njc0Mjc3MTJlIn19fQ==");
    public static final Item FINE_RUBY_GEMSTONE = head("fine_ruby_gemstone", "Fine Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY3Mjk1OTAyOGYyNzRiMzc5ZDQzMGYwNjhmMGYxNWE0Zjc5M2VhYzEyYWZiOTRhZTBiNGU1MGNmODk1ZGYwZiJ9fX0=");
    public static final Item FLAWLESS_RUBY_GEMSTONE = head("flawless_ruby_gemstone", "Flawless Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTI2YTI0OGZiYmMwNmNmMDZlMmM5MjBlY2ExY2FjOGEyYzk2MTY0ZDMyNjA0OTRiZWQxNDJkNTUzMDI2Y2M2In19fQ==");
    public static final Item PERFECT_RUBY_GEMSTONE = head("perfect_ruby_gemstone", "Perfect Ruby Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzliNmUwNDdkM2IyYmNhODVlOGNjNDllNTQ4MGY5Nzc0ZDhhMGVhZmU2ZGZhOTU1OTUzMDU5MDI4MzcxNTE0MiJ9fX0=");
    public static final Item ROUGH_AMETHYST_GEMSTONE = head("rough_amethyst_gemstone", "Rough Amethyst Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTQ5M2M2ZjU0MGM3MDAxZmVkOTdiMDdmNmI0Yzg5MTI4ZTNhN2MzNzU2M2FhMjIzZjBhY2NhMzE0ZjE3NTUxNSJ9fX0=");
    public static final Item FLAWED_AMETHYST_GEMSTONE = head("flawed_amethyst_gemstone", "Flawed Amethyst Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFkYjU5MjYwODk1NTc4ZDM3ZTU5NTA1ODgwNjAyZGU5NDBiMDg4ZTVmZmY4ZGEzZTY1MjAxZDczOWM4NmU4NCJ9fX0=");
    public static final Item FINE_AMETHYST_GEMSTONE = head("fine_amethyst_gemstone", "Fine Amethyst Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ExZWU1ZmZjZTA0ZWI3ZGE1OTJkNDI0MTRmZjM1ZTFiZjM4MTk0ZDZiODJlMzEwZGJjNjI2MWI0N2ZiOWM5MSJ9fX0=");
    public static final Item FLAWLESS_AMETHYST_GEMSTONE = head("flawless_amethyst_gemstone", "Flawless Amethyst Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDM2MjM1MjFjODExMWFkMjllOWRjZjdhY2M1NjA4NWE5YWIwN2RhNzMyZDE1MTg5NzZhZWU2MWQwYjNlM2JkNiJ9fX0=");
    public static final Item PERFECT_AMETHYST_GEMSTONE = head("perfect_amethyst_gemstone", "Perfect Amethyst Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDg4NmUwZjQxMTg1YjE4YTNhZmQ4OTQ4OGQyZWU0Y2FhMDczNTAwOTI0N2NjY2YwMzljZWQ2YWVkNzUyZmYxYSJ9fX0=");
    public static final Item ROUGH_AMBER_GEMSTONE = head("rough_amber_gemstone", "Rough Amber Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGExOTQzNmY2MTUxYTdiNjZkNjVlZDc2MjRhZGQ0MzI1Y2ZiYmMyZWVlODE1ZmNmNzZmNGMyOWRkZjA4Zjc1YiJ9fX0=");
    public static final Item FLAWED_AMBER_GEMSTONE = head("flawed_amber_gemstone", "Flawed Amber Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTczYmNmYzM5ZWI4NWRmMTg0ODUzNTk4NTIxNDA2MGExYmQxYjNiYjQ3ZGVmZTQyMDE0NzZlZGMzMTY3MTc0NCJ9fX0=");
    public static final Item FINE_AMBER_GEMSTONE = head("fine_amber_gemstone", "Fine Amber Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGIxY2NlMjJkZTE5ZWQ2NzI3YWJjNWU2YzJkNTc4NjRjODcxYTQ0Yzk1NmJiZTJlYjM5NjAyNjliNjg2YjhiMyJ9fX0=");
    public static final Item FLAWLESS_AMBER_GEMSTONE = head("flawless_amber_gemstone", "Flawless Amber Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWRjZTYyZjcwYWMwNDZiODgxMTEzYzZjZjg2Mjk4NzcyNzc3NGUyNjU4ODU1MDFjOWEyNDViMTgwZGIwOGMwZCJ9fX0=");
    public static final Item PERFECT_AMBER_GEMSTONE = head("perfect_amber_gemstone", "Perfect Amber Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdhZTIzNmNkZWMzZjJhNmY1MWVhZTE1ZTJjOGY2MjI4YjM0ZjEzN2RhMTU2OWZlYzllODAzZjljZDgxNzU5ZCJ9fX0=");
    public static final Item ROUGH_TOPAZ_GEMSTONE = head("rough_topaz_gemstone", "Rough Topaz Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2ZkOTYwNzIyZWMyOWM2NjcxNmFlNWNhOTdiOWI2YjI2Mjg5ODRlMWQ2ZjlkMjU5MmNkMDg5OTE0MjA2YTFiIn19fQ==");
    public static final Item FLAWED_TOPAZ_GEMSTONE = head("flawed_topaz_gemstone", "Flawed Topaz Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjYzOTI3NzNkMTE0YmUzMGFlYjNjMDljOTBjYmU2OTFmZmVhY2ViMzk5YjUzMGZlNmZiNTNkZGMwY2VkMzcxNCJ9fX0=");
    public static final Item FINE_TOPAZ_GEMSTONE = head("fine_topaz_gemstone", "Fine Topaz Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTJjYjZlNTFjNDYxZTczNTk1MjZiZWE1ZTA2MjA5Y2RkZGU3YzY0NjlhODE5ZjM0MDVjZjBhMDM4YzE1OTUwMiJ9fX0=");
    public static final Item FLAWLESS_TOPAZ_GEMSTONE = head("flawless_topaz_gemstone", "Flawless Topaz Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDEwOTY0ZjNjNDc5YWQ3ZDlhZmFmNjhhNDJjYWI3YzEwN2QyZDg4NGY1NzVjYWUyZjA3MGVjNmY5MzViM2JlIn19fQ==");
    public static final Item PERFECT_TOPAZ_GEMSTONE = head("perfect_topaz_gemstone", "Perfect Topaz Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2RhNmVjZGNiYzNmZTM1NWNhMDYxMTE5MmEzZmJkMzVkZDU2MzVkNWZjZGYzZmJjNzllZDJiYzFmNGEwMTdmZSJ9fX0=");
    public static final Item ROUGH_SAPPHIRE_GEMSTONE = head("rough_sapphire_gemstone", "Rough Sapphire Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2ZjZWJlNTRkYmMzNDVlYTdlMjIyMDZmNzAzZTZiMzNiZWZiZTk1YjZhOTE4YmQxNzU0Yjc2MTg4YmM2NWJiNSJ9fX0=");
    public static final Item FLAWED_SAPPHIRE_GEMSTONE = head("flawed_sapphire_gemstone", "Flawed Sapphire Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGEwYWY5OWU4ZDg3MDMxOTRhODQ3YTU1MjY4Y2Y1ZWY0YWM0ZWIzYjI0YzBlZDg2NTUxMzM5ZDEwYjY0NjUyOSJ9fX0=");
    public static final Item FINE_SAPPHIRE_GEMSTONE = head("fine_sapphire_gemstone", "Fine Sapphire Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYxNjFkYWEzNTg5ZWM5YzgxODc0NTlhYzM2ZmQ0ZGQyNjQ2YzA0MDY3OGQzYmZhY2I3MmEyMjEwYzZjODAxYyJ9fX0=");
    public static final Item FLAWLESS_SAPPHIRE_GEMSTONE = head("flawless_sapphire_gemstone", "Flawless Sapphire Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTU3Y2ZhOWM3NWJhNTg0NjQ1ZWUyYWY2ZDk4NjdkNzY3ZGRlYTQ2NjdjZGZjNzJkYzEwNjFkZDE5NzVjYTdkMCJ9fX0=");
    public static final Item PERFECT_SAPPHIRE_GEMSTONE = head("perfect_sapphire_gemstone", "Perfect Sapphire Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGU5M2ViYWNiNjBiNzE3OTMzNTVmZGUwZDRiYmE0M2E5YzVlYzA5YzNmMzg4OTdjNDhjMWY4NTc1MjNhMGEyOSJ9fX0=");
    public static final Item ROUGH_JADE_GEMSTONE = head("rough_jade_gemstone", "Rough Jade Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2I0YzJhZmQ1NDRkMGE2MTM5ZTZhZThlZjhmMGJmYzA5YTlmZDgzN2QwY2FkNGY1Y2QwZmU3ZjYwN2I3ZDFhMCJ9fX0=");
    public static final Item FLAWED_JADE_GEMSTONE = head("flawed_jade_gemstone", "Flawed Jade Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODIyODJjNmJiODM0M2UwZjBkNjFlZTA3NDdkYWRhNzUzNDRmMzMyZTlmZjBhY2FhM2FkY2RmMDkzMjFkM2RkIn19fQ==");
    public static final Item FINE_JADE_GEMSTONE = head("fine_jade_gemstone", "Fine Jade Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjI4ZjFjMGM1MDkyZTEyZDMzNzcwZGY0NWM1ODQ1YTk2MTA4ODYwMzliMzRhYmU5M2ExNmM1ZTk0MmRmYzhlNCJ9fX0=");
    public static final Item FLAWLESS_JADE_GEMSTONE = head("flawless_jade_gemstone", "Flawless Jade Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjg5Zjc1ZTBiMDAzNzhhNTgzZGJiYTcyOGRjZGM2ZTkzNDZmMzFkZDYwMWQ0NDhmM2Q2MDYxNWM3NDY1Y2MzZSJ9fX0=");
    public static final Item PERFECT_JADE_GEMSTONE = head("perfect_jade_gemstone", "Perfect Jade Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2ZjZWQ3OTc3MzgyYmY3MWQ0ZWUxN2ZmNWI5MTllMGViNzk3MjA4M2M0Y2NjZmExNzVjODc1M2FlNDBiYTAwNiJ9fX0=");
    public static final Item ROUGH_OPAL_GEMSTONE = head("rough_opal_gemstone", "Rough Opal Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTE0ZDNjNTdmODA4MjRiMzgzOWI4YjIyMGYyMTU4YmNhNTA1ZDQ5N2ZkMWM5ZTNmMjlmNDIyYjFlNjIwNmE0NSJ9fX0=");
    public static final Item FLAWED_OPAL_GEMSTONE = head("flawed_opal_gemstone", "Flawed Opal Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWFkYzNiY2RkN2M3MDFiNjNmOGI4YjRhOTZlNDI5MzE2YTA4Mzg4NjY5ZDlhOThjMWE5ODc5MTcyOWI5NjFkZiJ9fX0=");
    public static final Item FINE_OPAL_GEMSTONE = head("fine_opal_gemstone", "Fine Opal Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ3OThlMjBhNDdkMjUxYTllMzNkNDAzMzI5NzNjNzE4OWFjMTU1MDc2MGJhMjVjNGI5NTZjOTE1OTM2NDU2OCJ9fX0=");
    public static final Item FLAWLESS_OPAL_GEMSTONE = head("flawless_opal_gemstone", "Flawless Opal Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWQxNWVkNzBlNzIwMDQwYWQ3MzExZTY5MzU5ZGZkZjVlMTE0ZWFkZDJhNGMxZjk3MWE5NTAxMzQxYTQ1MjY0YiJ9fX0=");
    public static final Item PERFECT_OPAL_GEMSTONE = head("perfect_opal_gemstone", "Perfect Opal Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2U1MDlmMDZiOGRjMzg0MzU4ZmYyNDcyYWI2MmNlNmZkYzJmNjQ2ZTMzOGVmZGMzYzlmYjA1ZGRjNDMxZjY0In19fQ==");
    public static final Item ROUGH_JASPER_GEMSTONE = head("rough_jasper_gemstone", "Rough Jasper Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjNkMDY0ZWMxNTAxNzJkMDU4NDRjMTFhMTg2MTljMTQyMWJiZmIyZGRkMWRiYjg3Y2RjMTBlMjIyNTJiNzczYiJ9fX0=");
    public static final Item FLAWED_JASPER_GEMSTONE = head("flawed_jasper_gemstone", "Flawed Jasper Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTczNTExZTUwNGMzMTZiMTM5ZWRiMzVmZWJlNzNlZjU5MWMwZjQ1NWU4Y2FmOWVlMzUzYmMxMmI2YzE0YTkyMiJ9fX0=");
    public static final Item FINE_JASPER_GEMSTONE = head("fine_jasper_gemstone", "Fine Jasper Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWFjMTVmNmZjZjJjZTk2M2VmNGNhNzFmMWE4Njg1YWRiOTdlYjc2OWUxZDExMTk0Y2JiZDJlOTY0YTg4OTc4YyJ9fX0=");
    public static final Item FLAWLESS_JASPER_GEMSTONE = head("flawless_jasper_gemstone", "Flawless Jasper Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmY5OTNkM2E0M2Q0MDU5N2I0NzQ0ODU5NzYxNjBkMGNmNTJhYzY0ZDE1NzMwN2QzYjFjOTQxZGIyMjRkMGFjNiJ9fX0=");
    public static final Item PERFECT_JASPER_GEMSTONE = head("perfect_jasper_gemstone", "Perfect Jasper Gemstone",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjYzZjk5MWI4ZTAzOGU0NmI4ZWQ3NjMyZjQ0Y2EyZTMwYzE1ZjQyOTc3MDcwYThjOGQ4NzI4ZTNmYzA0ZmM3YyJ9fX0=");

    private GemstoneItems() {}

    private static Item head(String path, String displayName, String textureValue) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
        Property texture = new Property("textures", textureValue);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("hypixel:" + path).getBytes(StandardCharsets.UTF_8)),
                "HypixelGemstone", properties);
        Item.Properties itemProperties = new Item.Properties()
                .setId(key)
                .stacksTo(64)
                .component(DataComponents.CUSTOM_NAME, Component.literal(displayName))
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .component(DataComponents.PROFILE, ResolvableProfile.createResolved(profile))
                .component(DataComponents.LORE, new ItemLore(java.util.List.of(
                        ItemFormatting.lore(auraName(path)))));
        Item item = new SimplePolymerItem(itemProperties, Items.PLAYER_HEAD) {
            @Override
            public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack,
                                                   PacketContext context, HolderLookup.Provider lookup) {
                ResolvableProfile itemProfile = stack.get(DataComponents.PROFILE);
                if (itemProfile != null) out.set(DataComponents.PROFILE, itemProfile);
            }
        };
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static String auraName(String path) {
        if (path.contains("ruby")) return "Health";
        if (path.contains("amethyst")) return "Gemstone Fortune";
        if (path.contains("amber")) return "Strength";
        if (path.contains("topaz")) return "Crit Chance";
        if (path.contains("sapphire")) return "Intelligence";
        if (path.contains("jade")) return "Defense";
        if (path.contains("opal")) return "Crit Damage";
        return "Everything";
    }

    public static void initialize() {
        // Referencing this class initializes and registers all 40 gemstone items.
    }
}
