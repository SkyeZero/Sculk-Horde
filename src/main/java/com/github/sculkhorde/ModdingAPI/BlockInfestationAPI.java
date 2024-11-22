package com.github.sculkhorde.ModdingAPI;

import com.github.sculkhorde.common.block.InfestationEntries.BlockInfestationTable;
import com.github.sculkhorde.systems.BlockInfestationSystem;

import java.util.Comparator;

public class BlockInfestationAPI {


    public static BlockInfestationTable addBlockInfestationTable(BlockInfestationTable table)
    {
        BlockInfestationSystem.INFESTATION_TABLES.add(table);
        BlockInfestationSystem.INFESTATION_TABLES.sort(Comparator.comparing(BlockInfestationTable::getPriority));
        return table;
    }

    public static BlockInfestationTable getExplicitBlockInfestationTable()
    {
        return BlockInfestationSystem.explicitInfectableBlocks;
    }

    public static BlockInfestationTable getTagBlockInfestationTable()
    {
        return BlockInfestationSystem.tagInfectableBlocks;
    }

    public static BlockInfestationTable getTagNonFullBlockInfestationTable()
    {
        return BlockInfestationSystem.tagInfectableNonFullBlocks;
    }

    public static BlockInfestationTable geConfigBlockInfestationTable()
    {
        return BlockInfestationSystem.configInfectableBlocks;
    }
}
