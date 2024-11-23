package com.github.sculkhorde.util;

import com.github.sculkhorde.common.entity.infection.CursorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

public abstract class CursorQueue {

    ArrayList<BlockPos> positions = new ArrayList<>();
    ArrayList<CursorEntity> cursors = new ArrayList<>();

    int maxCursors = 32;

    Level level;
    EntityType<? extends CursorEntity> cursorType;

    public CursorQueue(Level world, EntityType<? extends CursorEntity> type) {
        level = world;
        cursorType = type;
    }

    public CursorQueue(EntityType<? extends CursorEntity> type) {
        cursorType = type;
    }

    public void setMaxCursors(int max) {
        maxCursors = max;
    }

    public void addLocation(BlockPos pos) {
        positions.add(pos);
        onAddLocation(pos);
    }

    public void removeLocation(BlockPos pos) {
        positions.remove(pos);
        onRemoveLocation(pos);
    }

    public void addCursor(CursorEntity cursor) {
        cursors.add(cursor);
        onAddCursor(cursor);
    }

    public void removeCursor(CursorEntity cursor) {
        cursors.remove(cursor);
        onRemoveCursor(cursor);
        if (!positions.isEmpty()) {
            int current = positions.size()-1;
            newCursor(positions.get(current));
            positions.remove(current);
        } else {
            onNoPositionsLeft();
        }
    }

    public void init() {
        for (int i = 0; i < maxCursors; i++) {
            int current = positions.size()-1;
            newCursor(positions.get(current));
            positions.remove(current);
        }
    }

    public void newCursor(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        CursorEntity cursor = cursorType.create(level);

        assert cursor != null;
        cursor.setPos(x + 0.5, y, z + 0.5);
        cursor.queue(this);

        onNewCursor(cursor);

        addCursor(cursor);
        level.addFreshEntity(cursor);
    }

    public void onAddCursor(CursorEntity cursor) {}
    public void onRemoveCursor(CursorEntity cursor) {}

    public void onAddLocation(BlockPos pos) {}
    public void onRemoveLocation(BlockPos pos) {}

    public void onNewCursor(CursorEntity cursor) {
        cursor.setTickIntervalMilliseconds(1);
        cursor.setSearchIterationsPerTick(1);
    }

    public void onNoPositionsLeft() {

    }

}
