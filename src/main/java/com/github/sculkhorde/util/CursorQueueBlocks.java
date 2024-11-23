package com.github.sculkhorde.util;

import com.github.sculkhorde.common.entity.infection.CursorEntity;
import com.github.sculkhorde.common.entity.infection.CursorTopDownInfectorEntity;
import com.github.sculkhorde.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CursorQueueBlocks extends CursorQueue {
    public CursorQueueBlocks(Level world, EntityType<? extends CursorEntity> type) {
        super(world, type);
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
}
