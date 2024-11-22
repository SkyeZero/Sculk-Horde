package com.github.sculkhorde.common.command;

import com.github.sculkhorde.util.ChunkInfestationHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ChunkCommand implements Command<CommandSourceStack> {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        return Commands.literal("chunk")
                .then(Commands.literal("infect")
                    .executes((context -> chunk(context, "infect")))
                )
                .then(Commands.literal("purify")
                        .executes((context -> chunk(context, "purify")))
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
            case "purify" -> {
                ChunkInfestationHelper.purifyChunk(level.getChunkAt(entity.blockPosition()), level);
            }
        }
        return 0;
    }

}


