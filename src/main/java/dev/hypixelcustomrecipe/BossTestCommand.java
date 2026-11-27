package dev.hypixelcustomrecipe;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Admin-only commands for testing individual boss encounters without progression. */
public final class BossTestCommand {
    private BossTestCommand() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("boss")
                        .requires(source -> source.getEntity() instanceof ServerPlayer player
                                && source.getServer().getPlayerList().isOp(player.nameAndId()))
                        .then(Commands.literal("summon")
                                .then(Commands.literal("maxor").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!MaxorEncounter.summonForTesting(player)) {
                                        context.getSource().sendFailure(Component.literal(
                                                "Could not summon Maxor. Another boss encounter may already be active."));
                                        return 0;
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("goldor").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!GoldorEncounter.summonForTesting(player)) {
                                        context.getSource().sendFailure(Component.literal(
                                                "Could not summon Goldor. Another boss encounter may already be active."));
                                        return 0;
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("necron").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!NecronEncounter.summonForTesting(player)) {
                                        context.getSource().sendFailure(Component.literal("Could not summon Necron. Another boss encounter may already be active."));
                                        return 0;
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                                .then(Commands.literal("storm").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!StormEncounter.summonForTesting(player)) {
                                        context.getSource().sendFailure(Component.literal(
                                                "Could not summon Storm. Another boss encounter may already be active."));
                                        return 0;
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))));
    }
}
