package com.github.sculkhorde.common.command;

import com.github.sculkhorde.core.SculkHorde;
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

public class DomainCommand implements Command<CommandSourceStack> {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        return Commands.literal("domain")
                .then(Commands.literal("expansion")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                .executes((context -> domain(context, "expansion")))
                        )
                )
                .then(Commands.literal("shatter")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes((context -> domain(context, "shatter")))
                        )
                )
                .then(Commands.literal("eject")
                        .executes((context -> domain(context, "eject")))
                );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int domain(CommandContext<CommandSourceStack> context, String operation) throws CommandSyntaxException {

        Entity entity = context.getSource().getEntityOrException();

        switch (operation) {

            case "expansion" -> {
                int id = SculkHorde.sculkDomainHandler.domainExpansion((ServerLevel) entity.level(), entity.blockPosition(), (Integer) context.getArgument("radius", Object.class), false);
                context.getSource().sendSuccess(()-> Component.literal("Domain Created! | ID: " + id), false);
            }
            case "shatter" -> {
                int id = (Integer) context.getArgument("id", Object.class);
                boolean success = SculkHorde.sculkDomainHandler.domainShatter(id);
                if (success) {
                    context.getSource().sendSuccess(()-> Component.literal("Destroyed Domain with ID: " + id), false);
                } else {
                    context.getSource().sendFailure(Component.literal("No Domain with ID: " + id));
                }
            }
            case "eject" -> {
                boolean success = SculkHorde.sculkDomainHandler.domainEject(context.getSource().getEntityOrException());
                if (success) {
                    context.getSource().sendSuccess(()-> Component.literal("Successfully ejected entity!"), false);
                } else {
                    context.getSource().sendFailure(Component.literal("Unknown Error, Check log for more details"));
                }
            }
        }
        return 0;
    }

}

/*

case "create_and_infect" -> {
                entity.playSound(ModSounds.ENDER_BUBBLE_LOOP.get());
                int id = SculkHorde.sphereManager.createAndInfect((ServerLevel) entity.level(), entity.blockPosition(), (Integer) context.getArgument("radius", Object.class));
                if (id > 0) {
                    context.getSource().sendSuccess(()-> Component.literal("ID: " + id), false);
                } else {
                    context.getSource().sendFailure(Component.literal("Something went wrong"));
                }
            }

case "infect" -> {
                int id = (Integer) context.getArgument("id", Object.class);
                boolean success = SculkHorde.sphereManager.infectSphere((ServerLevel) entity.level(), id);
                if (success) {
                    context.getSource().sendSuccess(()-> Component.literal("Infected Barrier with ID: " + id), false);
                } else {
                    context.getSource().sendFailure(Component.literal("Could not find barrier with ID: " + id));
                }
            }

Tasks.scheduleTask(entity.level(), 1, () -> {
                    entity.playSound(ModSounds.ENDER_BUBBLE_LOOP.get());
                });
                Tasks.scheduleTask(entity.level(), 40, () -> {
                    entity.playSound(SoundEvents.WARDEN_HEARTBEAT);
                });
                Tasks.scheduleTask(entity.level(), 60, () -> {
                    entity.playSound(SoundEvents.WARDEN_HEARTBEAT);
                });
 */


