package com.github.sculkhorde.common.entity.infection;

import com.github.sculkhorde.core.ModEntities;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.BlockInfestationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CursorTopDownInfectorEntity extends CursorEntity{
    /**
     * An Easier Constructor where you do not have to specify the Mob Type
     *
     * @param worldIn The world to initialize this mob in
     */
    public CursorTopDownInfectorEntity(Level worldIn) { this(ModEntities.CURSOR_TOP_DOWN_INFECTOR.get(), worldIn); }

    public CursorTopDownInfectorEntity(EntityType<?> pType, Level pLevel) {
        super(pType, pLevel);
    }

    public void setLowestY(int y) { lowestY = y;}
    public void setSpawnCursor(boolean s) { spawnCursor = s;}

    private int lowestY = -63;
    private boolean spawnCursor = false;
    private BlockPos lastExposedBlock = null;
    private BlockPos lastSpawnBlock = null;

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {return;}

        if(canBeManuallyTicked())
        {
            ticksRemainingBeforeCheckingIfInCursorList--;

            if(ticksRemainingBeforeCheckingIfInCursorList <= 0)
            {
                // SculkHorde.topDownCursorHandler.computeIfAbsent(this);
                ticksRemainingBeforeCheckingIfInCursorList = CHECK_DELAY_TICKS;
            }
        }

        /*
        boolean canBeManuallyTickedAndManualControlIsNotOn = (canBeManuallyTicked() && !SculkHorde.topDownCursorHandler.isManualControlOfTickingEnabled());
        boolean cannotBeManuallyTicked = !canBeManuallyTicked();

        boolean shouldTick = canBeManuallyTickedAndManualControlIsNotOn || cannotBeManuallyTicked;
         */

        boolean shouldTick = true;

        if(shouldTick) {
            cursorTick();
        }
    }

    @Override
    public void cursorTick() {
        float timeElapsedMilliSeconds = System.currentTimeMillis() - lastTickTime;
        double tickIntervalMillisecondsAfterMultiplier = tickIntervalMilliseconds;
        if (timeElapsedMilliSeconds < Math.max(tickIntervalMillisecondsAfterMultiplier, 1)) {
            return;
        }
        else {
            BlockPos currentPos = this.blockPosition();

            double x = currentPos.getX()+0.5;
            double z = currentPos.getZ()+0.5;

            // Am I currently in a Ward Block or next to one?
            Boolean isWarded = BlockAlgorithms.isExposedToInfestationWardBlock((ServerLevel) level(), currentPos);

            // Have I reached the lowest Y?
            Boolean reachedLowest = currentPos.getY() <= lowestY;

            if (!isWarded && level().getBlockState(this.blockPosition()).is(BlockTags.LEAVES)) {
                level().setBlockAndUpdate(this.blockPosition(), Blocks.AIR.defaultBlockState());
            }

            // Is current block or next block warded?
            if (isWarded) {

                if (reachedLowest) { snapSelf(); } // If reached the lowest block in the chunk, delete self
                else { this.setPos(x, currentPos.below(3).getY(), z); } // If not, skip past ward block area

            } else if (anyAirBlocks(level(), currentPos)) { // Is block exposed to air?

                lastExposedBlock = this.blockPosition();

                if (BlockInfestationHelper.isInfectable((ServerLevel) level(), currentPos)) { BlockInfestationHelper.tryToInfestBlock((ServerLevel) level(), currentPos); } // Infect if possible

                this.setPos(x, currentPos.below().getY(), z);

            } else {

                if (reachedLowest) { snapSelf(); } // If reached the lowest block in the chunk, delete self
                else { // If not, move down and spawn a surface cursor
                    if (lastSpawnBlock == null || lastSpawnBlock.getY()+4 < lastExposedBlock.getY()) {
                        // spawnSurfaceCursor(); // Entities aren't ticking when spawned in for some reason
                    }
                    this.setPos(x, currentPos.below().getY(), z);
                }
            }
        }

        lastTickTime = System.currentTimeMillis();

    }

    public void snapSelf() {
        // spawnSurfaceCursor(); // Entities aren't ticking when spawned in for some reason

        //SculkHorde.topDownCursorHandler.removeCursor(this);
        this.discard();
    }

    public void spawnSurfaceCursor() {
        if (spawnCursor && lastExposedBlock != null) {
            if (!BlockAlgorithms.isExposedToInfestationWardBlock((ServerLevel) level(), lastExposedBlock)) {

                CursorSurfaceInfectorEntity cursor = new CursorSurfaceInfectorEntity(level());
                cursor.setPos(lastExposedBlock.getX(), lastExposedBlock.getY() + 1, lastExposedBlock.getZ());

                cursor.setTickIntervalMilliseconds(3);
                cursor.setMaxTransformations(10);
                cursor.setMaxRange(50);
                cursor.setSearchIterationsPerTick(20);
                cursor.setMaxLifeTimeMillis(TimeUnit.SECONDS.toMillis(10));

                level().addFreshEntity(cursor);

                lastSpawnBlock = lastExposedBlock;
            }
        }
    }


    // Check if the specified block has any exposed faces
    public boolean anyAirBlocks(Level world, BlockPos pos) {
        ArrayList<BlockPos> list = BlockAlgorithms.getAdjacentNeighbors(pos);
        for(BlockPos position : list)
        {
            BlockState block = world.getBlockState(position);
            if(!BlockAlgorithms.isSolid((ServerLevel) level(), position)|| block.is(BlockTags.LEAVES)) {
                return true;
            }
        }
        return false;
    }


    // Unused
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag p_20052_) {}
    @Override protected void addAdditionalSaveData(CompoundTag p_20139_) {}
}
