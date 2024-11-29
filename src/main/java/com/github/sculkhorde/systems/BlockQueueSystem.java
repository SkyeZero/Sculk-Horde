package com.github.sculkhorde.systems;

import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.ChunkInfestationHelper;
import com.github.sculkhorde.util.TaskScheduler;
import com.github.sculkhorde.systems.AutoPerformanceSystem.PerformanceMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class BlockQueueSystem {
    ArrayList<BlockPos> blocks = new ArrayList<>();
    ArrayList<BlockPos> toInfect = new ArrayList<>();
    ArrayList<BlockPos> toClear = new ArrayList<>();
    ArrayList<BlockPos> cursorLocations = new ArrayList<>();
    ArrayList<LevelChunk> sectors;
    Level level;
    ServerLevel serverLevel;
    int lowest;

    int currentBlock = 0;
    int blocksSinceLastClear = 0;
    int trigger = 2304;

    int batch;
    int clearBatch;
    int checkBatch = 256;
    int batchCount = 0;

    boolean overridePerformance = false;
    boolean finished = false;
    PerformanceMode lastMode;

    public BlockQueueSystem(ArrayList<LevelChunk> chunks, Level world, int low, int batchSize) {
        sectors = chunks; level = world; serverLevel = (ServerLevel) world; lowest = low; batch = batchSize;
        startPrep();
    }

    boolean debug = true;
    boolean debugDetailed = false;

    public void log(String log) {
        if (debug) {SculkHorde.LOGGER.info(log);}
    }

    public void dLog(String log) {
        if (debugDetailed) {SculkHorde.LOGGER.info(log);}
    }

    public void startPrep() {
        log("-------- Prep Work Begin --------");
        if (batch > 0) {
            overridePerformance = true;
            log("   Override Performance: TRUE");
        } else {
            log("   Override Performance: FALSE");
        }

        updateBatches();
        log("   Batch: " + batch + " | Clear Batch: " + clearBatch);

        log("   Getting Sector Blocks...");
        for(LevelChunk sector : sectors) {
            ArrayList<BlockPos> heightMap = ChunkInfestationSystem.getHeightMap(sector);
            blocks.addAll(heightMap);
        }

        log("   Shuffling Blocks...");
        Collections.shuffle(blocks);

        log("   Getting Infectable & Clearable...");
        prepBlocks();
    }

    public void finishPrep() {
        log("   Clearing Settings...");
        finished = false;
        currentBlock = 0;
        batchCount = 0;
        blocksSinceLastClear = 0;

        log("--------  Prep Work End  --------");

        log("--------- Details ---------");
        log("   Submitted Blocks : " + blocks.size());
        log("   Total to Infect  : " + toInfect.size() + " | Batches required to infect: " + (toInfect.size()/batch));
        log("   Total to Clear   : " + toClear.size() + " | Batches required to clear: " + (toClear.size()/clearBatch));
        log("   True Lowest Block: " + lowest);
        log("--------- Details ---------");

        //log("");

        blocks.clear(); // Clear Memory

        log("Array [blocks] Cleared!: " + blocks);

        log("-------- Cleaning Area Start --------");
        cleanArea();
    }

    public void getBlocks() {
        for(LevelChunk sector : sectors) {
            ArrayList<BlockPos> heightMap = ChunkInfestationSystem.getHeightMap(sector);
            blocks.addAll(heightMap);
        }
    }

    public void prepBlocks() {
        log("       Batch " + (batchCount+1) + " Started | Batch Size: " + checkBatch + " Current Block: " + currentBlock + " | Size: " + blocks.size() + " Blocks Remaining: " + (blocks.size() - currentBlock));
        for (int i = 0; i < checkBatch; i++) {
            currentBlock++;
            if (currentBlock < blocks.size()) {

                BlockPos pos = blocks.get(currentBlock);
                dLog("          " + currentBlock + ": Checking Block @ [ X: " + pos.getX() + " | Z: " + pos.getZ() + " | Starting Y: " + pos.getY());
                prepBlock(pos);

            } else {
                finished = true;
                break;
            }
        }

        log("       Batch " + (batchCount+1) + " Complete");
        batchCount++;

        if (!finished) {
            updateBatches();
            TaskScheduler.scheduleTask(1, this::prepBlocks);
        } else {
            finishPrep();
        }
    }

    public void prepBlock(BlockPos pos) {
        BlockPos lastExposed = pos;
        for (int y = pos.getY(); y > level.getMinBuildHeight(); y--) {
            BlockPos newPos = new BlockPos(pos.getX(), y, pos.getZ());

            if (!BlockAlgorithms.isExposedToInfestationWardBlock(serverLevel, newPos)) {
                BlockState state = level.getBlockState(newPos);
                if (clearable(state)) {
                    dLog("       [" + newPos.getY() + " : " + state + "] | Block Result: Clearable");
                    toClear.add(newPos);
                }
                else if (BlockAlgorithms.isExposedToAir(serverLevel, newPos) && BlockInfestationSystem.isInfectable(serverLevel, newPos)){
                    dLog("       [" + newPos.getY() + " : " + state + "] | Block Result: Infectable");
                    toInfect.add(newPos);
                    lastExposed = newPos;
                }
                /*
                else if (y <= lowest) {
                    dLog("       [" + newPos.getY() + " : " + state + "] | Block Result: N/A & Final");
                    break;
                }
                 */
                else {
                    cursorLocations.add(newPos);
                    dLog("       [" + newPos.getY() + " : " + state + "] | Block Result: N/A");

                    break;
                }
            }
            else {
                dLog("       [" + newPos.getY() + " : UNKNOWN ] | Block Result: Warded & Final");
                break;
            }
        }
    }

    public boolean clearable(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.equals(Blocks.GRASS.defaultBlockState()) || state.equals(Blocks.BAMBOO.defaultBlockState());
    }

    public void cleanArea() {
        log("   Batch " + (batchCount+1) + " Started | Batch Size: " + clearBatch + " Current Block: " + currentBlock + " | Size: " + toClear.size() + " Blocks Remaining: " + (toClear.size() - currentBlock));
        for (int i = 0; i < clearBatch; i++) {
            currentBlock++;
            if (currentBlock < toClear.size()) {
                dLog("      " + toClear.get(currentBlock) + ": Clearing...");
                level.setBlockAndUpdate(toClear.get(currentBlock), Blocks.AIR.defaultBlockState());
            } else {
                finished = true;
                break;
            }
        }

        log("   Batch " + (batchCount+1) + " Complete");
        batchCount++;

        if (!finished) {
            if (blocksSinceLastClear >= trigger) {
                blocksSinceLastClear = 0;
                clear();
            } else {
                blocksSinceLastClear += clearBatch;
            }

            TaskScheduler.scheduleTask(1, this::cleanArea);
        } else {
            log("--------  Cleaning Area End  --------");

            toClear.clear();
            log("Array [toClear] Cleared!: " + toClear);
            log("Remove last dropped items");
            clear();

            finished = false;
            currentBlock = 0;
            batchCount = 0;
            blocksSinceLastClear = 0;

            log("Settings Cleared!");
            log("Begin Infection");

            next();
        }
    }

    public void next() {

        log("   Batch " + (batchCount+1) + " Started | Batch Size: " + batch + " Current Block: " + currentBlock + " | Size: " + toInfect.size() + " Blocks Remaining: " + (toInfect.size() - currentBlock));

        for (int i = 0; i < batch; i++) {
            currentBlock++;
            if (currentBlock < toInfect.size()) {
                if (!BlockAlgorithms.isExposedToInfestationWardBlock(serverLevel, toInfect.get(currentBlock))) {
                    dLog("      [" + currentBlock + " : " + toInfect.get(currentBlock) + ": Infecting...");
                    BlockInfestationSystem.tryToInfestBlock(serverLevel, toInfect.get(currentBlock));
                }
            } else {
                finished = true;
                break;
            }
        }

        log("   Batch " + (batchCount+1) + " Complete");
        batchCount++;

        if (!finished) {
            if (blocksSinceLastClear >= trigger) {
                blocksSinceLastClear = 0;

                clear();
            } else {
                blocksSinceLastClear += batch;
            }

            updateBatches();
            TaskScheduler.scheduleTask(1, this::next);
        } else {
            log("Infection Complete!");
            clear();
            toInfect.clear();
            log("Array [toInfect] Cleared!: " + toInfect);
            log("Potential Cursor Locations");
            for (BlockPos cursor : cursorLocations) {
                log("   Location: " + cursor + " | Block: " + level.getBlockState(cursor));
            }
        }

    }

    public int getBatch() {
        PerformanceMode performanceMode = SculkHorde.autoPerformanceSystem.getPerformanceMode();

        if (performanceMode != lastMode && !overridePerformance) {
            log("   Updating batch count | Reason: Performance Mode Updated | From: " + lastMode + " / To: " + performanceMode);
            lastMode = performanceMode;

            switch (performanceMode) {
                case High -> {
                    return 64;
                }
                case Medium -> {
                    return 32;
                }
                case Low -> {
                    return 16;
                }
                case Potato -> {
                    return 8;
                }
                default -> {
                    return 16;
                }
            }
        } else { return batch; }
    }

    public void updateBatches() {
        batch = getBatch();
        clearBatch = batch * 4; // Typical will be 256
        checkBatch = batch * 4; // Typical will be 256 - If so, worst case would be 256*384 blocks (98,304 blocks) in a tick. This shouldn't ever happen unless intentionally done by a player
    }

    public void clear() {
        log("   Clearing...");
        for (LevelChunk sector: sectors) {
            consumeItemsChunk(sector);
        }
        log("   Cleared " + itemCount + " items | Added " + mass + " mass");
        itemCount = 0;
        mass = 0;
    }

    int itemCount = 0;
    int mass = 0;

    private final Predicate<Entity> IS_DROPPED_ITEM = (entity) -> {return entity instanceof ItemEntity;};

    public void consumeItemsChunk(LevelChunk sector) {

        ChunkPos pos = sector.getPos();
        int minX = pos.getMinBlockX(); // Minimum block x in the chunk
        int minZ = pos.getMinBlockZ(); // Minimum block z in the chunk
        int maxX = pos.getMaxBlockX(); // Maximum block x in the chunk
        int maxZ = pos.getMaxBlockZ(); // Maximum block z in the chunk
        AABB chunkBounds = new AABB(minX, level.getMinBuildHeight(), minZ, maxX + 1, level.getMaxBuildHeight(), maxZ + 1);

        // Retrieve all entities in the chunk bounds
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, chunkBounds, IS_DROPPED_ITEM);

        for (ItemEntity item : items) {
            if(ModConfig.SERVER.isItemEdibleToCursors(item)) {
                int massToAdd = item.getItem().getCount();

                item.discard();

                itemCount++;
                mass += massToAdd;

                SculkHorde.savedData.addSculkAccumulatedMass(massToAdd);
                SculkHorde.statisticsData.addTotalMassFromInfestedCursorItemEating(massToAdd);
            }
        }

    }
}

/*
public void next() {

        for (int i = 0; i < batch; i++) {
            if (currentBlock + i < blocks.size()) {
                BlockPos pos = blocks.get(currentBlock + i);

                for (int y = pos.getY(); y > level.getMinBuildHeight(); y--) {
                    BlockPos newPos = new BlockPos(pos.getX(), y, pos.getZ());
                    if (level.getBlockState(newPos).is(BlockTags.LEAVES)) {
                        level.setBlockAndUpdate(newPos, Blocks.AIR.defaultBlockState());
                    }
                    if (BlockAlgorithms.isExposedToAir((ServerLevel) level, newPos)) {
                        BlockInfestationSystem.tryToInfestBlock((ServerLevel) level, newPos);
                    } else if (y <= lowest) {
                        break;
                    }
                }

            } else {
                finished = true;
                clear();

                break;
            }
        }

        if (!finished) {
            if (blocksSinceLastClear >= trigger) {
                blocksSinceLastClear = 0;
                currentBlock += batch;

                clear();
            } else {
                blocksSinceLastClear += batch;
                currentBlock += batch;
            }

            batch = getBatch();

            TaskScheduler.scheduleTask(1, this::next);
        } else {
            SculkHorde.LOGGER.info("Infection Complete!");
        }

    }
 */
