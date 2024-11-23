package com.github.sculkhorde.util.old;

import com.github.sculkhorde.common.entity.infection.CursorEntity;
import com.github.sculkhorde.util.ChunkInfestationHelper;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Collections;

public class ChunkInfectionQueue {

    private ArrayList<LevelChunk> chunks = new ArrayList<>();
    private LevelChunk currentChunk;

    public void addChunkToQueue(LevelChunk chunk) {
        chunks.add(chunk);
    }

    public void removeChunkFromQueue(LevelChunk chunk) {
        chunks.remove(chunk);
    }

    public void shuffleChunks() {
        Collections.shuffle(chunks);
    }

    private ArrayList<CursorEntity> cursors = new ArrayList<>();

    public void addCursor(CursorEntity cursor) {
        cursors.add(cursor);
    }

    public void removeCursor(CursorEntity cursor) {
        cursors.remove(cursor);
        checkCursors();
    }

    public void checkCursors() {
        if (cursors.isEmpty()) {
            removeChunkFromQueue(currentChunk);
            currentChunk = null;
            nextChunk();
        }
    }

    public void nextChunk() {
        if (currentChunk == null && !chunks.isEmpty()) {
            currentChunk = chunks.get(0);
            ChunkInfestationHelper.infectChunkTracked(currentChunk, currentChunk.getLevel(), this);
        }
    }

}
