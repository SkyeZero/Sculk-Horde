package com.github.sculkhorde.util.old;

import com.github.sculkhorde.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Map;

public class SphereGenerator {

    /**
     * Generates a hollow sphere of glass blocks in the world.
     *
     * @param world       The ServerLevel where the sphere will be placed.
     * @param center      The center position of the sphere.
     * @param radius      The radius of the sphere.
     * @param replacedMap A map to store replaced block positions and their states.
     */
    public static ArrayList<BlockPos> generateHollowSphere(ServerLevel world, BlockPos center, int radius, Map<BlockPos, BlockState> replacedMap) {

        ArrayList<BlockPos> blocksInside = new ArrayList<>();
        ArrayList<BlockPos> barrierBlocks = new ArrayList<>();

        // Loop over all positions within a cube that bounds the sphere
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distanceSquared = x * x + y * y + z * z;
                    double outerRadiusSquared = radius * radius;
                    double innerRadiusSquared = (radius - 1) * (radius - 1);

                    BlockPos pos = center.offset(x, y, z);

                    // Check if this position is on the shell of the sphere
                    if (distanceSquared <= innerRadiusSquared) {
                        if (!world.getBlockState(pos).isAir()) {
                            blocksInside.add(pos);
                        }
                    }
                    else if (distanceSquared <= outerRadiusSquared /*&& distanceSquared > innerRadiusSquared*/) {

                        Block state = world.getBlockState(pos).getBlock();

                        // Store the current block state if replacing
                        if (!state.equals(ModBlocks.BARRIER_OF_SCULK.get()) && !state.equals(ModBlocks.SOLID_BARRIER_OF_SCULK.get())) {
                            replacedMap.put(pos, world.getBlockState(pos));
                        }
                        barrierBlocks.add(pos);

                        /*
                        if (!world.isEmptyBlock(pos) && !world.getBlockState(pos).is(BlockTags.LEAVES) && BlockAlgorithms.isSolid(world, pos)) {
                            world.setBlock(pos, ModBlocks.SOLID_BARRIER_OF_SCULK.get().defaultBlockState(), 3);
                        } else {
                            world.setBlock(pos, ModBlocks.BARRIER_OF_SCULK.get().defaultBlockState(), 3);
                        }
                         */

                    }
                }
            }
        }

        new SphereQueueBarrier(barrierBlocks, world);

        return blocksInside;
    }

}
