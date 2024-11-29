package com.github.sculkhorde.util.old;

import com.github.sculkhorde.core.ModBlocks;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.domains.SculkDomainBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SphereManager {

    public SphereManager() {

    }



    // Tracks all created spheres and the blocks they replaced
    private final Map<Integer, Map<BlockPos, BlockState>> sphereRadiusData = new HashMap<>();
    private final Map<Integer, ArrayList<BlockPos>> sphereInternalData = new HashMap<>();

    /**
     * Generates a hollow sphere of glass blocks and stores the replaced blocks.
     *
     * @param world  The ServerLevel where the sphere will be placed.
     * @param center The center position of the sphere.
     * @param radius The radius of the sphere.
     * @return The ID of the created sphere.
     */
    public int createSphere(ServerLevel world, BlockPos center, int radius) {
        Map<BlockPos, BlockState> replacedMap = new HashMap<>();
        ArrayList<BlockPos> internal;
        internal = SphereGenerator.generateHollowSphere(world, center, radius, replacedMap);

        // Store the replaced blocks under a unique ID
        int sphereId = sphereRadiusData.size()+1;
        sphereRadiusData.put(sphereId, replacedMap);
        sphereInternalData.put(sphereId, internal);

        SculkHorde.LOGGER.info("Sphere created with ID: " + sphereId);
        return sphereId;
    }

    public boolean infectSphere(ServerLevel world, int sphereId) {
        if (!sphereRadiusData.containsKey(sphereId)) {
            SculkHorde.LOGGER.info("No sphere found with ID: " + sphereId);
            return false;
        }

        ArrayList<BlockPos> positions = sphereInternalData.get(sphereId);
        SculkHorde.LOGGER.info("Size: " + positions.size());
        new SphereQueueInfect(positions, world);

        /*
        for (BlockPos pos : positions) {
            if (BlockAlgorithms.isExposedToAir(world, pos)) {
                if (!BlockAlgorithms.isExposedToInfestationWardBlock(world, pos) && BlockInfestationSystem.isInfectable(world, pos)) {
                    BlockInfestationSystem.tryToInfestBlock(world, pos);
                }
            }
        }
         */

        return true;
    }

    /**
     * Undoes the placement of a sphere, restoring all replaced blocks.
     *
     * @param world    The ServerLevel where the sphere resides.
     * @param sphereId The ID of the sphere to undo.
     */
    public boolean undoSphere(ServerLevel world, int sphereId) {
        if (!sphereRadiusData.containsKey(sphereId)) {
            SculkHorde.LOGGER.info("No sphere found with ID: " + sphereId);
            return false;
        }

        Map<BlockPos, BlockState> replacedBlocks = sphereRadiusData.get(sphereId);

        // Restore each replaced block
        for (Map.Entry<BlockPos, BlockState> entry : replacedBlocks.entrySet()) {
            world.setBlock(entry.getKey(), entry.getValue(), 3);
            world.playSound(null, entry.getKey(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.1f, 1.0f);
        }

        //SculkHorde.sphereTickHandler.removeTracking(world, sphereId);

        // Remove the sphere data after restoration
        sphereRadiusData.remove(sphereId);
        sphereInternalData.remove(sphereId);

        SculkHorde.LOGGER.info("Sphere with ID " + sphereId + " has been undone.");
        return true;
    }

    public int createAndInfect(ServerLevel world, BlockPos center, int radius) {

        Map<BlockPos, BlockState> replacedMap = new HashMap<>();
        ArrayList<BlockPos> internal;

        int sphereId = sphereRadiusData.size()+1;

        internal = sphere(world, center, radius, replacedMap, sphereId);

        // Store the replaced blocks under a unique ID
        sphereRadiusData.put(sphereId, replacedMap);
        sphereInternalData.put(sphereId, internal);

        SculkHorde.LOGGER.info("Sphere created with ID: " + sphereId);

        return sphereId;
    }

    public ArrayList<BlockPos> sphere(ServerLevel world, BlockPos center, int radius, Map<BlockPos, BlockState> replacedMap, int id) {
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

        //SculkHorde.sphereTickHandler.addTracking(world, center, radius, id);

        //new SculkDomainBuilder(world, barrierBlocks, blocksInside);

        return blocksInside;
    }
}

