package com.github.sculkhorde.util;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber

public class TaskScheduler {
    private static final Map<Integer, Runnable> tasks = new HashMap<>();
    private static int currentTick = 0;

    /**
     * Schedule a task to run after a delay.
     *
     * @param delayInTicks The delay in ticks before the task runs.
     * @param task         The code to execute after the delay.
     */
    public static void scheduleTask(int delayInTicks, Runnable task) {
        tasks.put(currentTick + delayInTicks, task);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) { // Run at the end of each tick
            currentTick++;

            // Execute and remove tasks scheduled for the current tick
            if (tasks.containsKey(currentTick)) {
                tasks.get(currentTick).run();
                tasks.remove(currentTick);
            }
        }
    }
}
