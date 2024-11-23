package com.github.sculkhorde.util;

import com.github.sculkhorde.common.entity.infection.CursorTopDownInfectorEntity;
import com.github.sculkhorde.common.entity.infection.CursorTopDownPurifierEntity;
import com.github.sculkhorde.util.old.BlockInfectionQueue;
import com.github.sculkhorde.util.old.ChunkInfectionQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;

public class ChunkInfestationHelper {

    public static void infectChunk(LevelChunk chunk, Level level) {
        ArrayList<BlockPos> heightMap = getHeightMap(chunk);

        int lowest = chunk.getMaxBuildHeight(); // Init at max build height
        ChunkPos chunkPos = chunk.getPos();

        for (BlockPos pos : heightMap) {
            int y = pos.getY();
            if (y < lowest) {
                lowest = y;
            }
        }

        // Spawn a Top Down Infector at each block in the chunk, setting its Y position to the highest block in that X,Z coordinate
        for (BlockPos pos : heightMap) {
            CursorTopDownInfectorEntity cursor = new CursorTopDownInfectorEntity( level);
            cursor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            cursor.setTickIntervalMilliseconds(10);
            //cursor.setLowestY(lowest);

            if (pos.getX() == chunkPos.getMinBlockX() || pos.getX() == chunkPos.getMaxBlockX() || pos.getZ() == chunkPos.getMinBlockZ() || pos.getZ() == chunkPos.getMinBlockZ()) {
                //level.addFreshEntity(cursor);
            }

            level.addFreshEntity(cursor);
        }
    }

    public static void purifyChunk(LevelChunk chunk, Level level) {
        ArrayList<BlockPos> heightMap = getHeightMap(chunk);

        int lowest = chunk.getMaxBuildHeight(); // Init at max build height
        ChunkPos chunkPos = chunk.getPos();

        for (BlockPos pos : heightMap) {
            int y = pos.getY();
            if (y < lowest) {
                lowest = y;
            }
        }

        // Spawn a Top Down Purifier at each block in the chunk, setting its Y position to the highest block in that X,Z coordinate
        for (BlockPos pos : heightMap) {
            CursorTopDownPurifierEntity cursor = new CursorTopDownPurifierEntity(level);
            cursor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            cursor.setTickIntervalMilliseconds(10);
            //cursor.setLowestY(lowest);

            if (pos.getX() == chunkPos.getMinBlockX() || pos.getX() == chunkPos.getMaxBlockX() || pos.getZ() == chunkPos.getMinBlockZ() || pos.getZ() == chunkPos.getMinBlockZ()) {
                //cursor.setSpawnCursor(true);
            }

            level.addFreshEntity(cursor);
        }
    }

    public static ArrayList<BlockPos> getHeightMap(LevelChunk chunk) {

        ArrayList<BlockPos> heightMap = new ArrayList<>();
        ChunkPos chunkPos = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                // Get the highest Y value using the heightmap
                int highestY = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR).getFirstAvailable(x, z);
                BlockPos pos = new BlockPos(chunkPos.getMinBlockX()+x, highestY, chunkPos.getMinBlockZ()+z );
                heightMap.add(pos);
            }
        }

        return heightMap;
    }

    public static void infectChunkTracked(LevelChunk chunk, Level level, ChunkInfectionQueue queue) {
        ArrayList<BlockPos> heightMap = getHeightMap(chunk);

        int lowest = chunk.getMaxBuildHeight(); // Init at max build height
        ChunkPos chunkPos = chunk.getPos();

        for (BlockPos pos : heightMap) {
            int y = pos.getY();
            if (y < lowest) {
                lowest = y;
            }
        }

        // Spawn a Top Down Infector at each block in the chunk, setting its Y position to the highest block in that X,Z coordinate
        for (BlockPos pos : heightMap) {
            CursorTopDownInfectorEntity cursor = new CursorTopDownInfectorEntity( level);
            cursor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            cursor.setTickIntervalMilliseconds(10);
            cursor.queue(queue);

            queue.addCursor(cursor);
            //cursor.setLowestY(lowest);

            if (pos.getX() == chunkPos.getMinBlockX() || pos.getX() == chunkPos.getMaxBlockX() || pos.getZ() == chunkPos.getMinBlockZ() || pos.getZ() == chunkPos.getMinBlockZ()) {
                //level.addFreshEntity(cursor);
            }

            level.addFreshEntity(cursor);
        }
    }

    public static void infectChunkShuffled(LevelChunk chunk, Level level, int radius) {
        ArrayList<LevelChunk> chunks = getChunksInRadius(chunk, level, radius);
        ArrayList<BlockPos> blocks = new ArrayList<>();

        boolean init = false;

        int minX = 0;
        int maxX = 0;
        int minZ = 0;
        int maxZ = 0;

        for (LevelChunk sector: chunks) {
            ArrayList<BlockPos> heightMap = getHeightMap(sector);
            for (BlockPos pos : heightMap) {
                int x = pos.getX();
                int z = pos.getZ();
                if (!init) {
                    minX = x;
                    maxX = x;
                    minZ = z;
                    maxZ = z;
                    init = true;
                } else {
                    if (x < minX) { minX = x; }
                    if (x > maxX) { maxX = x; }
                    if (z < minZ) { minZ = z; }
                    if (z > maxZ) { maxZ = z; }
                }
                blocks.add(pos);
            }
        }

        Collections.shuffle(blocks);
        BlockInfectionQueue queue = new BlockInfectionQueue(level, blocks);
        queue.setChunkEdge(minX, maxX, minZ, maxZ);
        queue.init();

    }

    public static void infectSectorTracked(ArrayList<BlockPos> blocks, Level level, BlockInfectionQueue queue) {
        for (BlockPos pos : blocks) {
            CursorTopDownInfectorEntity cursor = new CursorTopDownInfectorEntity( level);
            cursor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            cursor.setTickIntervalMilliseconds(10);
            cursor.queue(queue);

            queue.addCursor(cursor);

            level.addFreshEntity(cursor);
        }
    }

    public static void infectChunkRadius(LevelChunk chunk, Level level, int radius) {
        ArrayList<LevelChunk> chunks = getChunksInRadius(chunk, level, radius);
        ChunkInfectionQueue queue = new ChunkInfectionQueue();

        for (LevelChunk sector: chunks) {
            queue.addChunkToQueue(sector);
        }

        queue.shuffleChunks();
        queue.nextChunk();
    }

    public static ArrayList<LevelChunk> getChunksInRadius(LevelChunk chunk, Level level, int radius) {
        int x = chunk.getPos().x;
        int z = chunk.getPos().z;

        ArrayList<LevelChunk> chunks = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = x + dx;
                int chunkZ = z + dz;

                // Retrieve the chunk
                ChunkAccess chunkAccess = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunkAccess instanceof LevelChunk chunkToAdd) {
                    chunks.add(chunkToAdd);
                }
            }
        }

        return chunks;
    }
}
