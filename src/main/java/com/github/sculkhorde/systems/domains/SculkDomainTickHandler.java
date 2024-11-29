package com.github.sculkhorde.systems.domains;

import com.github.sculkhorde.core.SculkHorde;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SculkDomainTickHandler {

    private final int CHECK_INTERVAL = 20; // Check every second (20 ticks)
    private int tickCounter = 0;

    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel world)) return;

        tickCounter++;
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;

            SculkHorde.sculkDomainHandler.domainCheck();

        }
    }
}

