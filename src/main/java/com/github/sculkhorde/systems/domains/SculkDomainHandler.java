package com.github.sculkhorde.systems.domains;

import com.github.sculkhorde.core.ModSounds;
import com.github.sculkhorde.core.SculkHorde;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SculkDomainHandler {

    private final Map<Integer, SculkDomain> trackedDomains = new HashMap<>();
    private final Map<BlockPos, SculkDomain> trackedNodeDomains = new HashMap<>();
    private int checkInterval = 20; // Check every second (20 ticks)
    private int tickCounter = 0;

    public SculkDomainHandler(int tickInterval) {
        checkInterval = tickInterval;
    }

    public void tickHandler(/*TickEvent.LevelTickEvent event*/) {
        //if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel world)) return;

        tickCounter++;
        if (tickCounter >= checkInterval) {
            tickCounter = 0;
            domainCheck();
        }
    }

    private final Map<String, Integer> playerAmbianceTicksA = new HashMap<>();
    private final Map<String, Integer> playerAmbianceTicksB = new HashMap<>();
    private final Map<String, Integer> playerAmbianceTicksC = new HashMap<>();
    private final Random r = new Random();

    public void ambianceHandler(/*TickEvent.ClientTickEvent event*/) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null || client.isPaused()) return;

        boolean withinDomain = false;

        for (Map.Entry<Integer, SculkDomain> entry : trackedDomains.entrySet()) {
            Set<Entity> entities = entry.getValue().getTrackedEntities();

            if(entities.contains(player)) {
                withinDomain = true;
                break;
            }
        }

        String uuid = player.getUUID().toString();
        int ticks = playerAmbianceTicksA.getOrDefault(uuid, 0);
        if (withinDomain) {
            if (ticks <= 0) {
                // Play sound and reset timer (example: 5 seconds interval)
                player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.ENDER_BUBBLE_LOOP.get(), SoundSource.AMBIENT, 0.2f, 0.6f, false);
                playerAmbianceTicksA.put(uuid, Mth.ceil(20 * 9));
            } else {
                playerAmbianceTicksA.put(uuid, ticks - 1);
            }
        } else {
            playerAmbianceTicksA.remove(uuid); // Clear tracking if player is no longer in a sphere
        }

        ticks = playerAmbianceTicksB.getOrDefault(uuid, 0);
        if (withinDomain) {
            if (ticks <= 0) {
                // Play sound and reset timer (example: 5 seconds interval)
                player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.SOUL_HARVESTER_ACTIVE.get(), SoundSource.AMBIENT, 0.8f, 1.0f, false);
                playerAmbianceTicksB.put(uuid, Mth.ceil(20 * 15));
            } else {
                playerAmbianceTicksB.put(uuid, ticks - 1);
            }
        } else {
            playerAmbianceTicksB.remove(uuid); // Clear tracking if player is no longer in a sphere
        }

        ticks = playerAmbianceTicksC.getOrDefault(uuid, 0);
        if (withinDomain) {
            if (ticks <= 0) {
                int ran = r.nextInt(8);
                BlockPos randomBlock = player.blockPosition().east(r.nextInt(10)).west(r.nextInt(10)).north(r.nextInt(10)).south(r.nextInt(10)).above(r.nextInt(10)).below(r.nextInt(10));
                switch (ran) {
                    case 0 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.SCULK_ENDERMAN_IDLE.get(), SoundSource.AMBIENT, 0.4f, 0.6f, false);
                    }
                    case 1 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.SCULK_ENDERMAN_SCREAM.get(), SoundSource.AMBIENT, 0.4f, 0.6f, false);
                    }
                    case 2 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.SCULK_ENDERMAN_STARE.get(), SoundSource.AMBIENT, 0.4f, 0.6f, false);
                    }
                    case 3 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.SCULK_ZOMBIE_IDLE.get(), SoundSource.AMBIENT, 0.4f, 0.6f, false);
                    }
                    case 4 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.NODE_SPAWN_SOUND.get(), SoundSource.AMBIENT, 0.4f, 0.6f, false);
                    }
                    case 5 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.NODE_DESTROY_SOUND.get(), SoundSource.AMBIENT, 0.4f, 0.6f, false);
                    }
                    case 6 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.BLIND_AND_ALONE_CORRUPTED.get(), SoundSource.AMBIENT, 1f, 1f, false);
                    }
                    case 7 -> {
                        player.clientLevel.playLocalSound(randomBlock, ModSounds.DEEP_GREEN_CORRUPTED.get(), SoundSource.AMBIENT, 1f, 1f, false);
                    }
                }
                /*
                    player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.SCULK_ENDERMAN_IDLE.get(), SoundSource.AMBIENT, 0.4f, 0.4f, false);
                    player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.SCULK_ENDERMAN_SCREAM.get(), SoundSource.AMBIENT, 0.4f, 0.4f, false);
                    player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.SCULK_ENDERMAN_STARE.get(), SoundSource.AMBIENT, 0.4f, 0.4f, false);
                    player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.SCULK_ZOMBIE_IDLE.get(), SoundSource.AMBIENT, 0.4f, 0.4f, false);
                    player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.NODE_SPAWN_SOUND.get(), SoundSource.AMBIENT, 0.4f, 0.4f, false);
                    player.clientLevel.playLocalSound(player.blockPosition(), ModSounds.NODE_DESTROY_SOUND.get(), SoundSource.AMBIENT, 0.4f, 0.4f, false);
                    player.clientLevel.playLocalSound(player.blockPosition().east(r.nextInt(10)), ModSounds.BLIND_AND_ALONE_CORRUPTED.get(), SoundSource.AMBIENT, 1f, 1f, false);
                    player.clientLevel.playLocalSound(player.blockPosition().west(r.nextInt(10)), ModSounds.BLIND_AND_ALONE_CORRUPTED.get(), SoundSource.AMBIENT, 1f, 1f, false);
                    player.clientLevel.playLocalSound(player.blockPosition().east(r.nextInt(10)), ModSounds.DEEP_GREEN_CORRUPTED.get(), SoundSource.AMBIENT, 1f, 1f, false);
                    player.clientLevel.playLocalSound(player.blockPosition().west(r.nextInt(10)), ModSounds.DEEP_GREEN_CORRUPTED.get(), SoundSource.AMBIENT, 1f, 1f, false);
                */

                playerAmbianceTicksC.put(uuid, Mth.ceil(20 * 15 + (r.nextInt(5))));
            } else {
                playerAmbianceTicksC.put(uuid, ticks - 1);
            }
        } else {
            playerAmbianceTicksC.remove(uuid); // Clear tracking if player is no longer in a sphere
        }
    }


    public Map<Integer, SculkDomain> getTrackedDomains() {
        return trackedDomains;
    }

    public void domainCheck() {
        for (Map.Entry<Integer, SculkDomain> entry : trackedDomains.entrySet()) {
            entry.getValue().domainCheckEntities();
        }
    }

    public void saveDomain(ServerLevel serverLevel, BlockPos center, int radius, int id) {

    }

    public void loadDomain(ServerLevel serverLevel, BlockPos center, int radius, int id) {
        //SculkDomain newDomain = new SculkDomain(serverLevel, center, radius);
        //trackedDomains.put(id, newDomain);
    }

    public int domainExpansion(ServerLevel serverLevel, BlockPos center, int radius, boolean breakable) {
        Random rand = new Random();
        int domainId = 0;

        while (domainId == 0 || trackedDomains.containsKey(domainId)) {
            domainId = rand.nextInt(9999);
        }

        SculkDomain newDomain = new SculkDomain(serverLevel, center, radius, breakable, domainId);
        trackedDomains.put(domainId, newDomain);

        newDomain.domainExpansion();

        return domainId;
    }

    public void nodeDomainExpansion(ServerLevel serverLevel, BlockPos node, int radius) {
        Random rand = new Random();
        int domainId = 0;

        while (domainId == 0 || trackedDomains.containsKey(domainId)) {
            domainId = rand.nextInt(9999);
        }

        SculkDomain newDomain = new SculkDomain(serverLevel, node.above(10), radius, true, domainId);
        trackedDomains.put(domainId, newDomain);
        trackedNodeDomains.put(node, newDomain);

        newDomain.domainExpansion();
    }

    public void nodeDomainShatter(BlockPos node) {
        if (trackedNodeDomains.containsKey(node)) {
            SculkDomain domain = trackedNodeDomains.get(node);
            trackedDomains.remove(domain.getId());
            trackedNodeDomains.remove(node);
            domain.domainShatter();
        }
    }

    public boolean domainShatter(int id) {
        if (trackedDomains.containsKey(id)) {
            trackedDomains.get(id).domainShatter();
            trackedDomains.remove(id);
            return true;
        } else {
            return false;
        }
    }

    public boolean domainEject(Entity entity) {
        boolean withinDomain = false;
        SculkDomain sculkDomain = null;

        for (Map.Entry<Integer, SculkDomain> entry : trackedDomains.entrySet()) {
            sculkDomain = entry.getValue();
            Set<Entity> entities = sculkDomain.getTrackedEntities();

            if(entities.contains(entity)) {
                withinDomain = true;
                break;
            }
        }

        if (sculkDomain == null) {
            SculkHorde.LOGGER.error("No active domains");
        } else if (!withinDomain) {
            SculkHorde.LOGGER.error("Entity is not within a tracked domain");
        }

        if (withinDomain && sculkDomain != null) {
            sculkDomain.domainEjectEntity(entity);
            return true;
        } else {
            return false;
        }
    }

}
