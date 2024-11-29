package com.github.sculkhorde.systems.domains;

import com.github.sculkhorde.core.ModBlocks;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.BlockInfestationSystem;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.TaskScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class SculkDomainBuilder {

    ArrayList<BlockPos> toBarrier;
    ArrayList<BlockPos> toCheck;
    ArrayList<BlockPos> toClear = new ArrayList<>();
    ArrayList<BlockPos> toInfect = new ArrayList<>();

    ArrayList<BlockPos> toPlacePos = new ArrayList<>();
    Map<BlockPos, BlockState> replaced;

    boolean breakableBarrier;

    Level level;
    ServerLevel serverLevel;

    int currentBlock = 0;
    int currentBarrierBlock = 0;
    int currentInfectionBlock = 0;

    int currentDissolveBlock = 0;

    boolean barrierMode = false;
    boolean checkFinished = false;
    boolean clearFinished = false;
    boolean barrierFinished = false;
    boolean infectFinished = false;

    boolean dissolveFinished = false;

    int barrierBatch = 512;
    int checkBatch = 10240;
    int clearBatch = 1024;
    int infectBatch = 256;

    int dissolveBatch = 128;

    BlockState clearBarrier;
    BlockState solidBarrier;

    public SculkDomainBuilder(Level world, ArrayList<BlockPos> barrierBlocks, ArrayList<BlockPos> internalBlocks, Boolean breakable) {
        level = world;
        serverLevel = (ServerLevel) level;

        toBarrier = barrierBlocks;
        toCheck = internalBlocks;

        if (breakable) {
            solidBarrier = ModBlocks.BREAKABLE_SOLID_BARRIER_OF_SCULK.get().defaultBlockState();
            clearBarrier = ModBlocks.BREAKABLE_BARRIER_OF_SCULK.get().defaultBlockState();
        } else {
            solidBarrier = ModBlocks.SOLID_BARRIER_OF_SCULK.get().defaultBlockState();
            clearBarrier = ModBlocks.BARRIER_OF_SCULK.get().defaultBlockState();
        }

        getBlocks();
    }

    public SculkDomainBuilder(Level world, Map<BlockPos, BlockState> map) {
        level = world;
        serverLevel = (ServerLevel) level;

        replaced = map;

        getReplacedBlocks();
    }

    public SculkDomainBuilder(Level world, ArrayList<BlockPos> barrierBlocks, Boolean breakable) {
        level = world;
        serverLevel = (ServerLevel) level;
        toBarrier = barrierBlocks;

        if (breakable) {
            solidBarrier = ModBlocks.BREAKABLE_SOLID_BARRIER_OF_SCULK.get().defaultBlockState();
            clearBarrier = ModBlocks.BREAKABLE_BARRIER_OF_SCULK.get().defaultBlockState();
        } else {
            solidBarrier = ModBlocks.SOLID_BARRIER_OF_SCULK.get().defaultBlockState();
            clearBarrier = ModBlocks.BARRIER_OF_SCULK.get().defaultBlockState();
        }

        regen();
    }

    public void regen() {
        if (currentBarrierBlock < toBarrier.size()) {
            BlockPos pos = toBarrier.get(currentBarrierBlock);
            if (!serverLevel.isEmptyBlock(pos) && !serverLevel.getBlockState(pos).is(BlockTags.LEAVES) && BlockAlgorithms.isSolid(serverLevel, pos)) {
                serverLevel.setBlock(pos, solidBarrier, 3);
            } else {
                serverLevel.setBlock(pos, clearBarrier, 3);
            }
        } else {
            barrierFinished = true;
        }
        currentBarrierBlock++;

        if (!barrierFinished) {
            TaskScheduler.scheduleTask(1, this::regen);
        }
    }

    public void getReplacedBlocks() {
        for (Map.Entry<BlockPos, BlockState> entry : replaced.entrySet()) {
            toPlacePos.add(entry.getKey());
        }
        Collections.shuffle(toPlacePos);
        dissolve();
    }

    public void dissolve() {
        if (!dissolveFinished) {
            for (int i = 0; i < dissolveBatch; i++) {
                if (currentDissolveBlock < toPlacePos.size()) {
                    BlockPos pos = toPlacePos.get(currentDissolveBlock);
                    BlockState state = replaced.get(pos);

                    serverLevel.setBlock(pos, state, 3);
                } else {
                    dissolveFinished = true;
                    break;
                }
                currentDissolveBlock++;
            }

            TaskScheduler.scheduleTask(1, this::dissolve);
        }
    }

    public boolean clearable(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.equals(Blocks.GRASS.defaultBlockState()) || state.equals(Blocks.BAMBOO.defaultBlockState());
    }

    public void reset() {
        currentBlock = 0;
    }

    public void getBlocks() {
        for (int i = 0; i < checkBatch; i++) {
            if (currentBlock < toCheck.size()) {
                BlockPos pos = toCheck.get(currentBlock);
                if (clearable(level.getBlockState(pos))) {
                    toClear.add(pos);
                }
                else if (BlockAlgorithms.isExposedToAir(serverLevel, pos)) {
                    if (!BlockAlgorithms.isExposedToInfestationWardBlock(serverLevel, pos) && BlockInfestationSystem.isInfectable(serverLevel, pos)) {
                        toInfect.add(pos);
                    }
                }
            } else {
                checkFinished = true;
                break;
            }
            currentBlock++;
        }

        if (!checkFinished) {
            TaskScheduler.scheduleTask(1, this::getBlocks);
        } else {
            Collections.shuffle(toClear);
            Collections.shuffle(toInfect);

            toCheck.clear();

            reset();
            clear();
        }
    }

    public void clear() {
        for (int i = 0; i < clearBatch; i++) {
            if (currentBlock < toClear.size()) {
                serverLevel.setBlockAndUpdate(toClear.get(currentBlock), Blocks.AIR.defaultBlockState());
            } else {
                clearFinished = true;
                break;
            }
            currentBlock++;
        }

        if (!clearFinished) {
            TaskScheduler.scheduleTask(1, this::clear);
        } else {
            toClear.clear();

            reset();
            run();
        }
    }

    public void run() {
        if (barrierFinished && infectFinished) {
            SculkHorde.LOGGER.info("Domain Complete!");
            return;
        } else {
            if (!barrierFinished && !infectFinished) {
                if (barrierMode) {
                    SculkHorde.LOGGER.info("Mode: Barrier");
                    barrierMode = false;
                    barrier();
                } else {
                    SculkHorde.LOGGER.info("Mode: Infection");
                    barrierMode = true;
                    infect();
                }
            } else if (barrierFinished) {
                SculkHorde.LOGGER.info("Mode: Infection");
                infect();
            } else {
                SculkHorde.LOGGER.info("Mode: Barrier");
                barrier();
            }
        }
    }

    public void barrier() {
        for (int i = 0; i < barrierBatch; i++) {
            if (currentBarrierBlock < toBarrier.size()) {
                BlockPos pos = toBarrier.get(currentBarrierBlock);
                if (!serverLevel.isEmptyBlock(pos) && !serverLevel.getBlockState(pos).is(BlockTags.LEAVES) && BlockAlgorithms.isSolid(serverLevel, pos)) {
                    serverLevel.setBlock(pos, solidBarrier, 3);
                } else {
                    serverLevel.setBlock(pos, clearBarrier, 3);
                }
            } else {
                barrierFinished = true;
                break;
            }
            currentBarrierBlock++;
        }

        TaskScheduler.scheduleTask(1, this::run);
    }


    public void infect() {
        for (int i = 0; i < infectBatch; i++) {
            if (currentInfectionBlock < toInfect.size()) {
                if (!BlockAlgorithms.isExposedToInfestationWardBlock(serverLevel, toInfect.get(currentInfectionBlock))) {
                    BlockInfestationSystem.tryToInfestBlock(serverLevel, toInfect.get(currentInfectionBlock));
                }
            } else {
                infectFinished = true;
                break;
            }
            currentInfectionBlock++;
        }

        TaskScheduler.scheduleTask(1, this::run);
    }
}
