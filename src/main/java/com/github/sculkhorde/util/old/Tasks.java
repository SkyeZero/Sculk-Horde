package com.github.sculkhorde.util.old;

import com.github.sculkhorde.core.SculkHorde;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

public class Tasks {

    public static void scheduleTask(Level level, int delay, Runnable task) {
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + delay, task::run));
    }

}
