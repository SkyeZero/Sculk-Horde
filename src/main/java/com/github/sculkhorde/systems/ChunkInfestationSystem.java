package com.github.sculkhorde.systems;

import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.TaskScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ChunkInfestationSystem {


    public static void infectChunk(LevelChunk chunk, Level level) {
        ArrayList<BlockPos> heightMap = getHeightMap(chunk);
        ArrayList<BlockPos> toInfect = new ArrayList<>();

        int lowest = chunk.getMaxBuildHeight();     // Init at max build height

        for (BlockPos pos : heightMap) {
            for (int y = pos.getY(); y > chunk.getMinBuildHeight(); y--) {
                if (!BlockAlgorithms.isExposedToAir((ServerLevel) level, new BlockPos(pos.getX(), y, pos.getZ())) && y <= lowest) {
                    break;
                } else if (y < lowest) {lowest = y;}
            }
        }

        for (BlockPos pos : heightMap) {
            for (int y = pos.getY(); y > chunk.getMinBuildHeight(); y--) {
                BlockPos newPos = new BlockPos(pos.getX(), y, pos.getZ());
                if (level.getBlockState(newPos).is(BlockTags.LEAVES)) { level.setBlockAndUpdate(newPos, Blocks.AIR.defaultBlockState()); }
                if (BlockAlgorithms.isExposedToAir((ServerLevel) level, newPos)) {
                    toInfect.add(newPos);
                } else if (y <= lowest) {
                    break;
                }
            }
        }

        for (BlockPos pos : toInfect) {
            BlockInfestationSystem.tryToInfestBlock((ServerLevel) level, pos);
        }

    }

    public static void infectChunkRadius(LevelChunk chunk, Level level, int radius, int batchSize) {
        ArrayList<LevelChunk> chunks = getChunksInRadius(chunk, level, radius);
        int lowest = chunk.getMaxBuildHeight();     // Init at max build height

        /*
        for (LevelChunk levelChunk : chunks) {
            ArrayList<BlockPos> heightMap = getHeightMap(levelChunk);
            int chunkLowest = chunk.getMaxBuildHeight();

            for (BlockPos pos : heightMap) {
                for (int y = pos.getY(); y > chunk.getMinBuildHeight(); y--) {
                    if (BlockAlgorithms.isExposedToAir((ServerLevel) level, new BlockPos(pos.getX(), y, pos.getZ()))) {
                        break;
                    } else if (y < chunkLowest) {
                        chunkLowest = y;
                    }
                }
            }

            for (BlockPos pos : heightMap) {
                for (int y = pos.getY(); y > chunk.getMinBuildHeight(); y--) {
                    if (BlockAlgorithms.isExposedToAir((ServerLevel) level, new BlockPos(pos.getX(), y, pos.getZ()))) {
                        if (y < lowest) {
                            lowest = y;
                        }
                    } else if (y <= chunkLowest) {
                        break;
                    }
                }
            }
        }
         */

        new BlockQueueSystem(chunks, level, lowest, batchSize);
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

    /*

    public static class ChunkQueue {
        ArrayList<LevelChunk> sectors;
        Level level;
        int lowest;

        public ChunkQueue(ArrayList<LevelChunk> chunks, Level world, int low) {
            sectors = chunks; level = world; lowest = low;
        }

        int current = 0;

        public void next() {
            if (current <= sectors.size()) {
                LevelChunk sector = sectors.get(current);
                infectChunk(sector, level, lowest);
            } else {
                clear();
            }
        }

        public void clear() {
            for (LevelChunk sector: sectors) {
                consumeItemsChunk(sector, level, lowest);
            }
        }

        public void infectChunk(LevelChunk chunk, Level level, int lowest) {
            ArrayList<BlockPos> heightMap = getHeightMap(chunk);
            ArrayList<BlockPos> toInfect = new ArrayList<>();

            for (BlockPos pos : heightMap) {
                for (int y = pos.getY(); y > chunk.getMinBuildHeight(); y--) {
                    BlockPos newPos = new BlockPos(pos.getX(), y, pos.getZ());
                    if (level.getBlockState(newPos).is(BlockTags.LEAVES)) { level.setBlockAndUpdate(newPos, Blocks.AIR.defaultBlockState()); }
                    if (BlockAlgorithms.isExposedToAir((ServerLevel) level, newPos)) {
                        toInfect.add(newPos);
                    } else if (y <= lowest) {
                        break;
                    }
                }
            }

            for (BlockPos pos : toInfect) {
                BlockInfestationSystem.tryToInfestBlock((ServerLevel) level, pos);
            }

            next();

        }

        public static void consumeItemsChunk(LevelChunk chunk, Level level, int lowest) {
            ChunkPos pos = chunk.getPos();
            int minX = pos.getMinBlockX(); // Minimum block x in the chunk
            int minZ = pos.getMinBlockZ(); // Minimum block z in the chunk
            int maxX = pos.getMaxBlockX(); // Maximum block x in the chunk
            int maxZ = pos.getMaxBlockZ(); // Maximum block z in the chunk
            AABB chunkBounds = new AABB(minX, lowest, minZ, maxX + 1, level.getMaxBuildHeight(), maxZ + 1);

            // Retrieve all entities in the chunk bounds
            List<Entity> entities = level.getEntities((Entity) null, chunkBounds, entity -> entity instanceof ItemEntity);

            for (Entity entity : entities) {
                if (entity instanceof ItemEntity itemEntity) {
                    if(!ModConfig.SERVER.isItemEdibleToCursors((ItemEntity) entity))
                    {
                        entity.discard();
                        int massToAdd = ((ItemEntity) entity).getItem().getCount();
                        SculkHorde.savedData.addSculkAccumulatedMass(massToAdd);
                        SculkHorde.statisticsData.addTotalMassFromInfestedCursorItemEating(massToAdd);
                    }
                }
            }
        }
    }

    public static class BlockQueue {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        ArrayList<AABB> boxes = new ArrayList<>();
        ArrayList<LevelChunk> sectors;
        Level level;
        int lowest;

        int currentBlock = 0;
        int currentRun = 0;
        int frequency;
        int batch;
        boolean finished = false;

        public BlockQueue(ArrayList<LevelChunk> chunks, Level world, int low, int batchSize) {
            sectors = chunks; level = world; lowest = low; batch = batchSize;

            for(LevelChunk sector : sectors) {
                ArrayList<BlockPos> heightMap = getHeightMap(sector);
                blocks.addAll(heightMap);
            }

            Collections.shuffle(blocks);

            createBoundingBoxes();
            frequency = getFrequency(batch);

            next();
        }

        public void createBoundingBoxes() {
            for (LevelChunk sector: sectors) {
                ChunkPos pos = sector.getPos();
                int minX = pos.getMinBlockX(); // Minimum block x in the chunk
                int minZ = pos.getMinBlockZ(); // Minimum block z in the chunk
                int maxX = pos.getMaxBlockX(); // Maximum block x in the chunk
                int maxZ = pos.getMaxBlockZ(); // Maximum block z in the chunk
                AABB chunkBounds = new AABB(minX, level.getMinBuildHeight(), minZ, maxX + 1, level.getMaxBuildHeight(), maxZ + 1);

                boxes.add(chunkBounds);
            }
        }

        public void next() {

            SculkHorde.LOGGER.info(currentRun + " | Next Batch of: " + batch + " blocks");

            for (int i = 0; i < batch; i++) {
                if (currentBlock + i < blocks.size()) {
                    BlockPos pos = blocks.get(currentBlock + i);

                    for (int y = pos.getY(); y > level.getMinBuildHeight(); y--) {
                        BlockPos newPos = new BlockPos(pos.getX(), y, pos.getZ());
                        if (level.getBlockState(newPos).is(BlockTags.LEAVES)) {
                            level.setBlockAndUpdate(newPos, Blocks.AIR.defaultBlockState());
                        }
                        if (BlockAlgorithms.isExposedToAir((ServerLevel) level, newPos)) {
                            BlockInfestationSystem.tryToInfestBlock((ServerLevel) level, newPos);
                        } else if (y <= lowest) {
                            break;
                        }
                    }

                } else {
                    finished = true;
                    clear();

                    break;
                }
            }

            if (!finished) {
                if (currentRun == frequency) {
                    currentRun = 0;
                    clear();
                }
                currentBlock += batch;
                TaskScheduler.scheduleTask(1, this::next);
            } else {
                SculkHorde.LOGGER.info("Infection Complete!");
            }

            currentRun++;
        }

        public int getFrequency(int batch) {
            if (batch <= 16) { return 32; }
            else if (batch <= 32) { return 16; }
            else if (batch <= 48) { return 11; }
            else if (batch <= 64) { return 8; }
            else if (batch <= 128) { return 4; }
            else if (batch <= 192) { return 3; }
            else if (batch <= 256) { return 2; }
            else { return 1; }
        }

        public void clear() {
            SculkHorde.LOGGER.info("Clearing...");
            for (AABB box: boxes) {
                consumeItemsChunk(level, box);
            }
            SculkHorde.LOGGER.info("Clearing Done");
        }

        int itemCount = 0;
        int mass = 0;

        private final Predicate<Entity> IS_DROPPED_ITEM = (entity) -> {return entity instanceof ItemEntity;};

        public void consumeItemsChunk(Level level, AABB chunkBounds) {
            // Retrieve all entities in the chunk bounds
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, chunkBounds, IS_DROPPED_ITEM);

            //SculkHorde.LOGGER.info("    Clearing Bounding Box: " + chunkBounds);

            for (ItemEntity item : items) {
                if(ModConfig.SERVER.isItemEdibleToCursors(item)) {
                    int massToAdd = item.getItem().getCount();

                    //SculkHorde.LOGGER.info("    Item: " + item + " | Mass: " + massToAdd);

                    item.discard();

                    itemCount++;
                    mass += massToAdd;

                    SculkHorde.savedData.addSculkAccumulatedMass(massToAdd);
                    SculkHorde.statisticsData.addTotalMassFromInfestedCursorItemEating(massToAdd);
                }
            }
        }
    }



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
