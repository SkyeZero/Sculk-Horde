package com.github.sculkhorde.common.entity.infection;

import com.github.sculkhorde.core.ModBlocks;
import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.ModEntities;
import com.github.sculkhorde.systems.BlockInfestationSystem;
import com.github.sculkhorde.util.BlockAlgorithms;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.github.sculkhorde.util.BlockAlgorithms.isExposedToInfestationWardBlock;

public class CursorTopDownPurifierEntity extends CursorSurfacePurifierEntity{
    /**
     * An Easier Constructor where you do not have to specify the Mob Type
     *
     * @param worldIn The world to initialize this mob in
     */
    public CursorTopDownPurifierEntity(Level worldIn) { this(ModEntities.CURSOR_TOP_DOWN_INFECTOR.get(), worldIn); }

    public CursorTopDownPurifierEntity(EntityType<?> pType, Level pLevel) {
        super(pType, pLevel);
    }


    /**
     * The only change I made was instead of adding all the neighbors to the queue,
     * just add the block below us.
     * @return
     */
    @Override
    protected boolean searchTick() {
        // Initialize the visited positions map and the queue
        // Complete 20 times.
        for (int i = 0; i < Math.max(searchIterationsPerTick, 1); i++)
        {
            // Breadth-First Search

            if (searchQueue.isEmpty()) {
                isSuccessful = false;
                target = BlockPos.ZERO;
                return true;
            }

            BlockPos currentBlock = searchQueue.poll();

            // If the current block is a target, return it
            if (isTarget(currentBlock)) {
                isSuccessful = true;
                target = currentBlock;
                return true;
            }

            // Add only below neighbor to queue
            addPositionToQueueIfValid(currentBlock.below());
        }

        return false;
    }

    /**
     * Returns true if the block is considered obstructed.
     * @param state the block state
     * @param pos the block position
     * @return true if the block is considered obstructed
     */
    @Override
    protected boolean isObstructed(BlockState state, BlockPos pos)
    {
        if(!ModConfig.SERVER.block_infestation_enabled.get())
        {
            return true;
        }
        else if(isExposedToInfestationWardBlock((ServerLevel) this.level(), pos))
        {
            return true;
        }

        // Check if block is not beyond world border
        if(!level().isInWorldBounds(pos))
        {
            return true;
        }

        // This is to prevent the entity from getting stuck in a loop
        if(visitedPositons.containsKey(pos.asLong()))
        {
            return true;
        }

        boolean isBlockNotExposedToAir = !BlockAlgorithms.isExposedToAir((ServerLevel) this.level(), pos);
        boolean isBlockNotSculkArachnoid = !state.is(ModBlocks.SCULK_ARACHNOID.get());
        boolean isBlockNotSculkDuraMatter = !state.is(ModBlocks.SCULK_DURA_MATTER.get());

        if(isBlockNotExposedToAir && isBlockNotSculkArachnoid && isBlockNotSculkDuraMatter)
        {
            return true;
        }

        return false;
    }
}
