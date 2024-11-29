package com.github.sculkhorde.common.command;

import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.ChunkInfestationSystem;
import com.github.sculkhorde.util.ChunkInfestationHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ChunkCommand implements Command<CommandSourceStack> {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        return Commands.literal("chunk")
                .then(Commands.literal("infect")
                    .executes((context -> chunk(context, "infect")))
                )
                .then(Commands.literal("infect_b")
                        .executes((context -> chunk(context, "infect_b")))
                )
                .then(Commands.literal("purify")
                        .executes((context -> chunk(context, "purify")))
                )
                .then(Commands.literal("infect_radius")
                        .then(Commands.literal("shuffled")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes((context -> chunk(context, "infect_radius_shuffled")))
                                )
                        )
                        .then(Commands.literal("chunky")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes((context -> chunk(context, "infect_radius")))
                                )
                        )
                )
                .then(Commands.literal("infect_radius_b")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                .then(Commands.argument("batch_size", IntegerArgumentType.integer(0))
                                    .executes((context -> chunk(context, "infect_radius_b")))
                                )
                        )
                )
                .then(Commands.literal("purify_radius")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                .executes((context -> chunk(context, "purify_radius")))
                        )
                );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int chunk(CommandContext<CommandSourceStack> context, String operation) throws CommandSyntaxException {

        Entity entity = context.getSource().getEntityOrException();
        Level level = entity.level();

        switch (operation) {

            case "infect" -> {
                ChunkInfestationHelper.infectChunk(level.getChunkAt(entity.blockPosition()), level);
            }
            case "infect_b" -> {
                ChunkInfestationSystem.infectChunk(level.getChunkAt(entity.blockPosition()), level);
            }
            case "purify" -> {
                ChunkInfestationHelper.purifyChunk(level.getChunkAt(entity.blockPosition()), level);
            }
            case "infect_radius" -> {
                ChunkInfestationHelper.infectChunkRadius(level.getChunkAt(entity.blockPosition()), level, (Integer) context.getArgument("value", Object.class));
            }
            case "infect_radius_b" -> {
                ChunkInfestationSystem.infectChunkRadius(level.getChunkAt(entity.blockPosition()), level, (Integer) context.getArgument("radius", Object.class), (Integer) context.getArgument("batch_size", Object.class));
            }
            case "infect_radius_shuffled" -> {
                ChunkInfestationHelper.infectChunkShuffled(level.getChunkAt(entity.blockPosition()), level, (Integer) context.getArgument("value", Object.class));
            }
            case "purify_radius" -> {
                //ChunkInfestationHelper.infectChunkRadius(level.getChunkAt(entity.blockPosition()), level, (Integer) context.getArgument("value", Object.class));
            }
        }
        return 0;
    }

}


