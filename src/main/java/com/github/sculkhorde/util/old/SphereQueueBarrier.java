package com.github.sculkhorde.util.old;

import com.github.sculkhorde.core.ModBlocks;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.TaskScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

public class SphereQueueBarrier {
    ArrayList<BlockPos> toChange = new ArrayList<>();

    Level level;
    ServerLevel serverLevel;

    int currentBlock = 0;
    int batch = 512;

    boolean finished = false;

    public SphereQueueBarrier(ArrayList<BlockPos> blocks, Level world) {
        toChange = blocks; level = world; serverLevel = (ServerLevel) level;
        run();
    }

    public void run() {
        for (int i = 0; i < batch; i++) {
            currentBlock++;
            if (currentBlock < toChange.size()) {
                BlockPos pos = toChange.get(currentBlock);
                if (!serverLevel.isEmptyBlock(pos) && !serverLevel.getBlockState(pos).is(BlockTags.LEAVES) && BlockAlgorithms.isSolid(serverLevel, pos)) {
                    serverLevel.setBlock(pos, ModBlocks.SOLID_BARRIER_OF_SCULK.get().defaultBlockState(), 3);
                } else {
                    serverLevel.setBlock(pos, ModBlocks.BARRIER_OF_SCULK.get().defaultBlockState(), 3);
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
