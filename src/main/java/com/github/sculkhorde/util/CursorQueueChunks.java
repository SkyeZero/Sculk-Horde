package com.github.sculkhorde.util;

import com.github.sculkhorde.common.entity.infection.CursorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;

public class CursorQueueChunks extends CursorQueue {
    public CursorQueueChunks(EntityType<? extends CursorEntity> type) {
        super(type);
    }

    private ArrayList<LevelChunk> chunks = new ArrayList<>();
    private LevelChunk currentChunk = null;

    public void addChunk(LevelChunk chunk) {
        chunks.add(chunk);
        onAddLocation(chunk);
    }

    public void removeChunk(LevelChunk chunk) {
        chunks.remove(chunk);
        onRemoveLocation(chunk);
    }

    public void queueBlocks(LevelChunk chunk) {
        ArrayList<BlockPos> blocks = ChunkInfestationHelper.getHeightMap(chunk);
        for (BlockPos pos: blocks) {
            addLocation(pos);
        }
    }

    private boolean spawnCursors = false;

    private int minX = 0;
    private int maxX = 0;
    private int minZ = 0;
    private int maxZ = 0;

    public void setChunkEdge(int x1, int x2, int z1, int z2) {
        minX = x1; maxX = x2; minZ = z1; maxZ = z2;
    }

    public void shouldSpawnSurfaceCursors(boolean state) {
        spawnCursors = state;
    }

    @Override
    public void newCursor(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        CursorEntity cursor = cursorType.create(level);

        assert cursor != null;
        cursor.setPos(x + 0.5, y, z + 0.5);
        cursor.queue(this);

        boolean edgeBlock = (x == minX || x == maxX || z == minZ || z == maxZ);

        if (edgeBlock) {
            cursor.setShouldSpawnCursor(spawnCursors);
        }

        onNewCursor(cursor);

        addCursor(cursor);
        level.addFreshEntity(cursor);
    }

    @Override
    public void onNoPositionsLeft() {
        currentChunk = null;
        initChunks();
    }

    public void initChunks() {
        if (currentChunk == null) {

            currentChunk = chunks.get(0);
            removeChunk(currentChunk);

            queueBlocks(currentChunk);
            init();
        }
    }

    public void onAddLocation(LevelChunk chunk) {}
    public void onRemoveLocation(LevelChunk chunk) {}

}
