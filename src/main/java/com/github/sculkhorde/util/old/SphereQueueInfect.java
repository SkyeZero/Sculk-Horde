package com.github.sculkhorde.util.old;

import com.github.sculkhorde.systems.BlockInfestationSystem;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.TaskScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;

public class SphereQueueInfect {
    ArrayList<BlockPos> toInfect = new ArrayList<>();
    ArrayList<BlockPos> toClear = new ArrayList<>();
    ArrayList<BlockPos> toCheck = new ArrayList<>();

    Level level;
    ServerLevel serverLevel;

    int currentBlock = 0;
    int batch = 64;

    boolean finished = false;

    public SphereQueueInfect(ArrayList<BlockPos> blocks, Level world) {
        level = world; serverLevel = (ServerLevel) level;
        toCheck = blocks;
        getBlocks();
    }

    public boolean clearable(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.equals(Blocks.GRASS.defaultBlockState()) || state.equals(Blocks.BAMBOO.defaultBlockState());
    }

    public void getBlocks() {
        for (int i = 0; i < 512; i++) {
            currentBlock++;
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
                finished = true;
                break;
            }
        }

        if (!finished) {
            TaskScheduler.scheduleTask(1, this::getBlocks);
        } else {
            currentBlock = 0;
            finished = false;

            Collections.shuffle(toClear);
            Collections.shuffle(toInfect);

            toCheck.clear();

            clear();
        }
    }

    public void clear() {
        for (int i = 0; i < batch; i++) {
            currentBlock++;
            if (currentBlock < toClear.size()) {
                serverLevel.setBlockAndUpdate(toClear.get(currentBlock), Blocks.AIR.defaultBlockState());
            } else {
                finished = true;
                break;
            }
        }

        if (!finished) {
            TaskScheduler.scheduleTask(1, this::clear);
        } else {
            currentBlock = 0;
            finished = false;

            toClear.clear();

            run();
        }
    }

    public void run() {
        for (int i = 0; i < batch; i++) {
            currentBlock++;
            if (currentBlock < toInfect.size()) {
                if (!BlockAlgorithms.isExposedToInfestationWardBlock(serverLevel, toInfect.get(currentBlock))) {
                    BlockInfestationSystem.tryToInfestBlock(serverLevel, toInfect.get(currentBlock));
                }
            } else {
                finished = true;
                break;
            }
        }

        if (!finished) {
            TaskScheduler.scheduleTask(1, this::run);
        }
    }


}
