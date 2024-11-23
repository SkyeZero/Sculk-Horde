package com.github.sculkhorde.systems;

import com.github.sculkhorde.common.entity.infection.CursorTopDownInfectorEntity;
import com.github.sculkhorde.core.ModEntities;
import com.github.sculkhorde.util.CursorQueueChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;

public class ChunkInfestationSystem {

    /*
    public static void infectChunk(LevelChunk chunk, Level level, Boolean spawnCursors) {
        ArrayList<BlockPos> heightMap = getHeightMap(chunk);
        ChunkPos chunkPos = chunk.getPos();

        CursorQueueChunks ChunkQueue = new CursorQueueChunks(level, ModEntities.CURSOR_TOP_DOWN_INFECTOR.get());

        // Spawn a Top Down Infector at each block in the chunk, setting its Y position to the highest block in that X,Z coordinate
        for (BlockPos pos : heightMap) {
            CursorTopDownInfectorEntity cursor = new CursorTopDownInfectorEntity( level);
            cursor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            cursor.setTickIntervalMilliseconds(10);

            boolean edgeBlock = (pos.getX() == chunkPos.getMinBlockX() || pos.getX() == chunkPos.getMaxBlockX() || pos.getZ() == chunkPos.getMinBlockZ() || pos.getZ() == chunkPos.getMinBlockZ());

            if (edgeBlock) {
                cursor.
            }

            level.addFreshEntity(cursor);
        }
    }

    public static void purifyChunk(LevelChunk chunk, Level level, Boolean spawnCursors) {

    }

    public static void infectChunkBlocks(LevelChunk chunk, Level level, int radius, Boolean spawnCursors) {

    }

    public static void purifyChunkBlocks(LevelChunk chunk, Level level, int radius, Boolean spawnCursors) {

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

     */

}
