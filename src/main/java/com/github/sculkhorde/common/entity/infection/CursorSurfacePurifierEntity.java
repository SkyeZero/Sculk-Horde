package com.github.sculkhorde.common.entity.infection;

import com.github.sculkhorde.core.ModBlocks;
import com.github.sculkhorde.core.ModEntities;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.systems.BlockInfestationSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import static com.github.sculkhorde.util.BlockAlgorithms.isExposedToInfestationWardBlock;

public class CursorSurfacePurifierEntity extends CursorEntity{

    /**
     * An Easier Constructor where you do not have to specify the Mob Type
     * @param worldIn  The world to initialize this mob in
     */
    public CursorSurfacePurifierEntity(Level worldIn)
    {
        this(ModEntities.CURSOR_SURFACE_PURIFIER.get(), worldIn);
    }

    public CursorSurfacePurifierEntity(EntityType<?> pType, Level pLevel) {
        super(pType, pLevel);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_20052_) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_20139_) {

    }


    @Override
    public boolean canBeManuallyTicked()
    {
        return false; // Purifiers should never be manually ticked
    }

    /**
     * Returns true if the block is considered obstructed.
     * @param pos the block position
     * @return true if the block is considered obstructed
     */
    @Override
    protected boolean isTarget(BlockPos pos)
    {
        return BlockInfestationSystem.isCurable((ServerLevel) level(), pos);
    }

    /**
     * Transforms the block at the given position.
     * @param pos the position of the block
     */
    @Override
    protected void transformBlock(BlockPos pos)
    {
        BlockInfestationSystem.tryToCureBlock((ServerLevel) this.level(), pos);

        // Get all infector cursor entities in area and kill them
        Predicate<CursorSurfaceInfectorEntity> isCursor = Objects::nonNull;
        List<CursorSurfaceInfectorEntity> Infectors = this.level().getEntitiesOfClass(CursorSurfaceInfectorEntity.class, this.getBoundingBox().inflate(5.0D), isCursor);
        for(CursorSurfaceInfectorEntity infector : Infectors)
        {
            level().getServer().tell(new TickTask(level().getServer().getTickCount() + 1, () -> {
                infector.discard();
                this.discard();
            }));
            break;
        }
    }

    @Override
    protected void spawnParticleEffects()
    {
        Random random = new Random();
        float maxOffset = 2;
        float randomXOffset = random.nextFloat(maxOffset * 2) - maxOffset;
        float randomYOffset = random.nextFloat(maxOffset * 2) - maxOffset;
        float randomZOffset = random.nextFloat(maxOffset * 2) - maxOffset;
        this.level().addParticle(ParticleTypes.TOTEM_OF_UNDYING, getX() + randomXOffset, getY() + randomYOffset, getZ() + randomZOffset, randomXOffset * 0.1, randomYOffset * 0.1, randomZOffset * 0.1);
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

        if(BlockAlgorithms.getBlockDistance(origin, pos) > MAX_RANGE)
        {
            return true;
        }

        if(state.isAir())
        {
            return true;
        }

        // If we detect fluid
        else if(!state.getFluidState().isEmpty())
        {
            // If its water, its only obstructed if its the water source block or flowing water block
            if(state.getFluidState().is(Fluids.WATER) && state.is(Blocks.WATER))
            {
                return true;
            }

            if(!state.getFluidState().is(Fluids.WATER))
            {
                return true;
            }
        }

        else if(isExposedToWardBlock((ServerLevel) this.level(), pos)) {
            return true;
        }

        // This is to prevent the entity from getting stuck in a loop
        if(visitedPositons.containsKey(pos.asLong()))
        {
            return true;
        }

        if(!BlockAlgorithms.isExposedToAir((ServerLevel) this.level(), pos))
        {
            return true;
        }

        return false;
    }

    public static boolean isExposedToWardBlock(ServerLevel serverWorld, BlockPos targetPos)
    {
        BlockState target = serverWorld.getBlockState(targetPos);

        Block clearBarrier = ModBlocks.BARRIER_OF_SCULK.get();
        Block solidBarrier = ModBlocks.SOLID_BARRIER_OF_SCULK.get();
        Block breakableClearBarrier = ModBlocks.BREAKABLE_BARRIER_OF_SCULK.get();
        Block breakableSolidBarrier = ModBlocks.BREAKABLE_SOLID_BARRIER_OF_SCULK.get();


        if(target.is(clearBarrier) || target.is(solidBarrier) || target.is(breakableClearBarrier) || target.is(breakableSolidBarrier)) {
            return true;
        }

        ArrayList<BlockPos> list = BlockAlgorithms.getAdjacentNeighbors(targetPos);

        for(BlockPos position : list)
        {
            BlockState currentTarget = serverWorld.getBlockState(position);
            if(currentTarget.is(clearBarrier) || currentTarget.is(solidBarrier) || currentTarget.is(breakableClearBarrier) || currentTarget.is(breakableSolidBarrier))
            {
                return true;
            }
        }

        return false;
    }
}
