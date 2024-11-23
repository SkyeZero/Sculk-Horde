package com.github.sculkhorde.util.old;

import com.github.sculkhorde.common.entity.infection.CursorEntity;
import com.github.sculkhorde.common.entity.infection.CursorSurfaceInfectorEntity;
import com.github.sculkhorde.common.entity.infection.CursorTopDownInfectorEntity;
import com.github.sculkhorde.core.ModEntities;
import com.github.sculkhorde.core.SculkHorde;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BlockInfectionQueue {

    /*
    private final ArrayList<ArrayList<BlockPos>> sectors = new ArrayList<>();
    private ArrayList<CursorEntity> cursors = new ArrayList<>();
    private final Level level;

    private ArrayList<BlockPos> currentSector = null;

    public BlockInfectionQueue(Level worldIn) {
        level = worldIn;
    }

    public void addSectorToQueue(ArrayList<BlockPos> sector) {
        sectors.add(sector);
    }

    public void removeSectorFromQueue(ArrayList<BlockPos> sector) {
        sectors.remove(sector);
    }
     */

    private final ArrayList<BlockPos> blocks;
    private final Level level;

    private final ArrayList<CursorEntity> cursors = new ArrayList<>();
    private final ArrayList<CursorEntity> surfaceCursors = new ArrayList<>();
    private final ArrayList<BlockPos> blocksForSurface = new ArrayList<>();

    private int minX = 0;
    private int maxX = 0;
    private int minZ = 0;
    private int maxZ = 0;

    public void setChunkEdge(int x1, int x2, int z1, int z2) {
        minX = x1; maxX = x2; minZ = z1; maxZ = z2;
    }

    public BlockInfectionQueue(Level worldIn, ArrayList<BlockPos> blocksToAdd) {
        level = worldIn;
        blocks = blocksToAdd;
    }

    public void init() {
        SculkHorde.LOGGER.info(this + " | Init()!");
        for (int i = 0; i < 64; i++) {
            newCursor(blocks.get(0));
            blocks.remove(0);
        }
    }

    public void newCursor(BlockPos pos) {

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        CursorTopDownInfectorEntity cursor = new CursorTopDownInfectorEntity(level);
        cursor.setPos(x + 0.5, y, z + 0.5);

        cursor.setTickIntervalMilliseconds(1);
        cursor.setSearchIterationsPerTick(1);
        cursor.queue(this);

        if (x == minX || x == maxX || z == minZ || z == maxZ) {
            cursor.setShouldSpawnCursor(true);
        }

        addCursor(cursor);
        level.addFreshEntity(cursor);
    }


    public void addCursor(CursorEntity cursor) {
        cursors.add(cursor);
    }

    public void removeCursor(CursorEntity cursor) {
        //SculkHorde.LOGGER.info(this + "| Blocks Status: "  + blocks.size() + " Blocks Remaining..");
        cursors.remove(cursor);
        if (!blocks.isEmpty()) {
            newCursor(blocks.get(0));
            blocks.remove(0);
            //offset++;
        }
        if(cursors.isEmpty()) {
            spawnSurfaceCursors();
        }
    }

    public void spawnSurfaceCursors() {
        Random r = new Random();
        int min = 6;
        int max = 10;

        for (BlockPos pos: blocksForSurface) {
            double x = pos.getX()+0.5;
            double y = pos.getY();
            double z = pos.getZ()+0.5;

            int rand = r.nextInt(max-min) + min;

            CursorSurfaceInfectorEntity infector = new CursorSurfaceInfectorEntity(ModEntities.CURSOR_SURFACE_INFECTOR.get(), level);
            infector.setPos(x, y, z);

            infector.setTickIntervalMilliseconds(3);
            infector.setSearchIterationsPerTick(20);
            infector.setMaxTransformations(rand);
            infector.setMaxRange(20);
            //infector.setMaxLifeTimeMillis(TimeUnit.SECONDS.toMillis(5));

            level.addFreshEntity(infector);
        }
    }

    public void queueSurfaceCursor(BlockPos pos) {
        blocksForSurface.add(pos);
    }

}
