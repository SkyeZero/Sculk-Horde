package com.github.sculkhorde.util;

import com.github.sculkhorde.common.entity.infection.CursorTopDownInfectorEntity;
import com.github.sculkhorde.common.entity.infection.CursorTopDownPurifierEntity;
import com.github.sculkhorde.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;

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
}
