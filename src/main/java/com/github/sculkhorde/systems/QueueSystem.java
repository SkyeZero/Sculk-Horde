package com.github.sculkhorde.systems;

import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.systems.AutoPerformanceSystem.PerformanceMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;

public class QueueSystem {

    ArrayList<LevelChunk> chunks;
    ArrayList<BlockPos> toChange = new ArrayList<>();
    ArrayList<BlockPos> toClear = new ArrayList<>();

    Level level;
    ServerLevel serverLevel;

    int blocksToTriggerItemClear = 2304; // 9 Chunks worth of blocks
    int totalXZBlocksChanged;
    int totalBlocksChanged;

    int searchesPerTick;
    int cleanersPerTick;
    int changesPerTick;

    boolean searchComplete = false;
    boolean cleanComplete = false;
    boolean changesComplete = false;

    PerformanceMode lastMode;
    boolean autoPerformance = true;
    boolean shuffleBlocks;

    boolean debug = true;
    boolean debugDetailed = false;

    public void log(String log) {
        if (debug) {SculkHorde.LOGGER.info(log);}
    }

    public void dLog(String log) {
        if (debugDetailed) {SculkHorde.LOGGER.info(log);}
    }

    public boolean clearable(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.equals(Blocks.GRASS.defaultBlockState()) || state.equals(Blocks.BAMBOO.defaultBlockState());
    }

    public boolean changeable(BlockPos pos) {
        return (BlockInfestationSystem.isInfectable(serverLevel, pos) && BlockAlgorithms.isExposedToAir(serverLevel, pos));
    }

    public boolean obstructed(BlockPos pos) {
        return (BlockAlgorithms.isExposedToInfestationWardBlock(serverLevel, pos));
    }

    public void transformBlock(BlockPos pos) {
        onTransformBlock();
    }

    public void onTransformBlock() {

    }

    public void clearBlock(BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        onClearBlock();
    }

    public void onClearBlock() {

    }

    public QueueSystem(Level world, boolean shouldShuffleBlocks) {
        level = world;
        serverLevel = (ServerLevel) level;

        shuffleBlocks = shouldShuffleBlocks;
    }

    public QueueSystem(Level world, boolean shouldShuffleBlocks, int searchesPerTickOverride, int cleanersPerTickOverride, int changesPerTickOverride ) {
        level = world;
        serverLevel = (ServerLevel) level;

        shuffleBlocks = shouldShuffleBlocks;

        autoPerformance = false;
        searchesPerTick = searchesPerTickOverride;
        cleanersPerTick = cleanersPerTickOverride;
        changesPerTick = changesPerTickOverride;

    }


    // Auto Peformance Override Functions
    public void setAutoPerformanceState(boolean autoPerformance) {
        this.autoPerformance = autoPerformance;
    }

    public boolean getAutoPerformanceState() {
        return autoPerformance;
    }

    public boolean setPerTickSystem(int searchesPerTickOverride, int cleanersPerTickOverride, int changesPerTickOverride) {
        if (!autoPerformance) {
            searchesPerTick = searchesPerTickOverride;
            cleanersPerTick = cleanersPerTickOverride;
            changesPerTick = changesPerTickOverride;
            return true;
        } else {
            SculkHorde.LOGGER.error("Warning from: " + this + "; Auto Performance Enabled, Cannot override!");
            return false;
        }
    }

    // Auto Performance System
    protected void updateTickSystem() {
        PerformanceMode performanceMode = SculkHorde.autoPerformanceSystem.getPerformanceMode();

        int newSearchesPerTick;
        int newCleanersPerTick;
        int newChangesPerTick;

        if (performanceMode != lastMode && autoPerformance) {
            lastMode = performanceMode;

            switch (performanceMode) {
                case High -> {
                    newSearchesPerTick = 512;
                    newCleanersPerTick = 256;
                    newChangesPerTick = 64;
                }
                case Medium -> {
                    newSearchesPerTick = 256;
                    newCleanersPerTick = 128;
                    newChangesPerTick = 32;
                }
                case Low -> {
                    newSearchesPerTick = 128;
                    newCleanersPerTick = 64;
                    newChangesPerTick = 16;
                }
                case Potato -> {
                    newSearchesPerTick = 64;
                    newCleanersPerTick = 32;
                    newChangesPerTick = 8;
                }
                default -> {
                    newSearchesPerTick = 200;
                    newCleanersPerTick = 100;
                    newChangesPerTick = 20;
                }
            }

            searchesPerTick = newSearchesPerTick;
            cleanersPerTick = newCleanersPerTick;
            changesPerTick = newChangesPerTick;

        }
    }

    public void addChunk() {

    }

    public void removeChunk() {

    }

    public void init() {

    }

    public void getChunkBlocks(LevelChunk chunk) {

        ArrayList<BlockPos> heightMap = ChunkInfestationSystem.getHeightMap(chunk);
        ArrayList<BlockPos> clearable = new ArrayList<>();
        ArrayList<BlockPos> modify = new ArrayList<>();

        int lowestY = chunk.getMaxBuildHeight();
        int heighestY = chunk.getMinBuildHeight();

        for (BlockPos pos : heightMap) {
            int currentY = 0;
            if (pos.getY() > heighestY) { heighestY = pos.getY(); }
            for (int y = pos.getY(); y > level.getMinBuildHeight(); y--) {

                BlockPos newPos = new BlockPos(pos.getX(), y, pos.getZ());

                if (!obstructed(newPos)) {
                    if (clearable(newPos)) {
                        clearable.add(newPos);
                        currentY = y;
                    }
                    else if (changeable(newPos)) {
                        modify.add(newPos);
                        currentY = y;
                    }
                    else {break;}
                } else {break;}
            }
            if (currentY < lowestY) { lowestY = currentY; }
        }

        for (BlockPos pos : heightMap) {
            for (int y = lowestY; y < heighestY; y++) {
                BlockPos newPos = new BlockPos(pos.getX(), y, pos.getZ());

                if (!obstructed(newPos)) {
                    if (clearable(newPos) && !clearable.contains(newPos)) {
                        clearable.add(newPos);
                    }
                    else if (changeable(newPos) && !modify.contains(newPos)) {
                        modify.add(newPos);
                    }
                    else {break;}
                } else {break;}
            }
        }

        toChange.addAll(modify);
        toClear.addAll(clearable);
    }

    public void clearArea() {

    }

    public void changeBlocks() {

    }

}
