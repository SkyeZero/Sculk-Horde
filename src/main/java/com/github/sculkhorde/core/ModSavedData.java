package com.github.sculkhorde.core;

import com.github.sculkhorde.common.block.SculkBeeNestBlock;
import com.github.sculkhorde.common.block.SculkNodeBlock;
import com.github.sculkhorde.common.blockentity.SculkNodeBlockEntity;
import com.github.sculkhorde.core.gravemind.Gravemind;
import com.github.sculkhorde.core.gravemind.RaidData;
import com.github.sculkhorde.core.gravemind.RaidHandler;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.StatisticsData;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.sculkhorde.util.BlockAlgorithms.getBlockDistance;
import static com.github.sculkhorde.util.BlockAlgorithms.getBlockDistanceXZ;

/**
 * This class handels all data that gets saved to and loaded from the world. <br>
 * Learned World Data mechanics from: https://www.youtube.com/watch?v=tyTsdCzVz6w
 */
public class ModSavedData extends SavedData {

    // identifier for debugmode
    private static final String debugModeIdentifier = "debugMode";

    //The world
    public final ServerLevel level;
    // List of all known positions of nodes.
    private final ArrayList<NodeEntry> nodeEntries = new ArrayList<>();
    // List of all known positions of bee nests
    private final ArrayList<BeeNestEntry> beeNestEntries = new ArrayList<>();
    // List of all known hostile entity types
    private final Map<String, HostileEntry> hostileEntries = new HashMap<>();
    // List of all known priority blocks
    private final ArrayList<PriorityBlockEntry> priorityBlockEntries = new ArrayList<>();
    // List of areas where sculk mobs have died.
    private final ArrayList<DeathAreaEntry> deathAreaEntries = new ArrayList<>();
    // List of areas of interests
    private final ArrayList<AreaofInterestEntry> areasOfInterestEntries = new ArrayList<>();
    // List of No Raid Zone Areas
    private final ArrayList<NoRaidZoneEntry> noRaidZoneEntries = new ArrayList<>();

    // the amount of mass that the sculk hoard has accumulated.
    private int sculkAccumulatedMass = 0;
    // used to write/read nbt data to/from the world.
    private static final String sculkAccumulatedMassIdentifier = "sculkAccumulatedMass";
    // The amount of ticks since sculk node destruction
    private int ticksSinceSculkNodeDestruction = Gravemind.TICKS_BETWEEN_NODE_SPAWNS;
    private static final String ticksSinceSculkNodeDestructionIdentifier = "ticksSinceSculkNodeDestruction";
    // The amount of ticks since last raid
    private int ticksSinceLastRaid = TickUnits.convertHoursToTicks(8);
    private static final String ticksSinceLastRaidIdentifier = "ticksSinceLastRaid";

    /**
     * Default Constructor
     */
    public ModSavedData()
    {
        level = ServerLifecycleHooks.getCurrentServer().overworld();
    }

    /**
     * Get the memory object that stores all the data
     * @return The GravemindMemory
     */
    private static @NotNull ModSavedData getGravemindMemory()
    {
        return SculkHorde.savedData;
    }


    /**
     * This method gets called every time the world loads data from memory.
     * We extract data from the memory and store it in variables.
     *
     * @param nbt The memory where data is stored
     */
    public static ModSavedData load(CompoundTag nbt) {

        CompoundTag gravemindData = nbt.getCompound("gravemindData");

        SculkHorde.savedData = new ModSavedData();

        SculkHorde.savedData.getNodeEntries().clear();
        SculkHorde.savedData.getBeeNestEntries().clear();
        SculkHorde.savedData.getHostileEntries().clear();
        SculkHorde.savedData.getPriorityBlockEntries().clear();
        SculkHorde.savedData.getDeathAreaEntries().clear();
        SculkHorde.savedData.getAreasOfInterestEntries().clear();

        SculkHorde.savedData.setSculkAccumulatedMass(nbt.getInt(sculkAccumulatedMassIdentifier));
        SculkHorde.savedData.setTicksSinceSculkNodeDestruction(nbt.getInt(ticksSinceSculkNodeDestructionIdentifier));

        SculkHorde.savedData.setTicksSinceLastRaid(nbt.getInt(ticksSinceLastRaidIdentifier));

        SculkHorde.setDebugMode(nbt.getBoolean(debugModeIdentifier));

        for (int i = 0; gravemindData.contains("node_entry" + i); i++) {
            SculkHorde.savedData.getNodeEntries().add(NodeEntry.serialize(gravemindData.getCompound("node_entry" + i)));
        }

        for (int i = 0; gravemindData.contains("bee_nest_entry" + i); i++) {
            SculkHorde.savedData.getBeeNestEntries().add(BeeNestEntry.serialize(gravemindData.getCompound("bee_nest_entry" + i)));
        }

        for (int i = 0; gravemindData.contains("hostile_entry" + i); i++) {
            HostileEntry hostileEntry = HostileEntry.serialize(gravemindData.getCompound("hostile_entry" + i));
            SculkHorde.savedData.getHostileEntries().putIfAbsent(hostileEntry.identifier, hostileEntry);
        }

        for (int i = 0; gravemindData.contains("priority_block_entry" + i); i++) {
            SculkHorde.savedData.getPriorityBlockEntries().add(PriorityBlockEntry.serialize(gravemindData.getCompound("priority_block_entry" + i)));
        }

        for (int i = 0; gravemindData.contains("death_area_entry" + i); i++) {
            SculkHorde.savedData.getDeathAreaEntries().add(DeathAreaEntry.serialize(gravemindData.getCompound("death_area_entry" + i)));
        }

        for (int i = 0; gravemindData.contains("area_of_interest_entry" + i); i++) {
            SculkHorde.savedData.getAreasOfInterestEntries().add(AreaofInterestEntry.serialize(gravemindData.getCompound("area_of_interest_entry" + i)));
        }

        for(int i = 0; gravemindData.contains("no_raid_zone_entry" + i); i++) {
            SculkHorde.savedData.getNoRaidZoneEntries().add(NoRaidZoneEntry.serialize(gravemindData.getCompound("no_raid_zone_entry" + i)));
        }

        if(RaidHandler.raidData == null)
        {
            RaidHandler.raidData = new RaidData();
        }

        if(SculkHorde.statisticsData == null)
        {
            SculkHorde.statisticsData = new StatisticsData();
        }

        StatisticsData.load(nbt);
        RaidData.load(nbt);

        return getGravemindMemory();

    }

    public ArrayList<NoRaidZoneEntry> getNoRaidZoneEntries() {
        return noRaidZoneEntries;
    }

    /**
     * This method gets called every time the world saves data from memory.
     * We take the data in our variables and store it to memory.
     *
     * @param nbt The memory where data is stored
     */
    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        CompoundTag gravemindData = new CompoundTag();

        nbt.putInt(sculkAccumulatedMassIdentifier, sculkAccumulatedMass);
        nbt.putInt(ticksSinceSculkNodeDestructionIdentifier, ticksSinceSculkNodeDestruction);
        nbt.putInt(ticksSinceLastRaidIdentifier, ticksSinceLastRaid);
        nbt.putBoolean(debugModeIdentifier, SculkHorde.isDebugMode());

        for (ListIterator<NodeEntry> iterator = getNodeEntries().listIterator(); iterator.hasNext(); ) {
            gravemindData.put("node_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<BeeNestEntry> iterator = getBeeNestEntries().listIterator(); iterator.hasNext(); ) {
            gravemindData.put("bee_nest_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        int hostileIndex = 0;
        for (Map.Entry<String, HostileEntry> entry : getHostileEntries().entrySet()) {
            gravemindData.put("hostile_entry" + hostileIndex, entry.getValue().deserialize());
            hostileIndex++;
        }

        for (ListIterator<PriorityBlockEntry> iterator = getPriorityBlockEntries().listIterator(); iterator.hasNext(); ) {
            gravemindData.put("priority_block_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<DeathAreaEntry> iterator = getDeathAreaEntries().listIterator(); iterator.hasNext(); ) {
            gravemindData.put("death_area_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<AreaofInterestEntry> iterator = getAreasOfInterestEntries().listIterator(); iterator.hasNext(); ) {
            gravemindData.put("area_of_interest_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<NoRaidZoneEntry> iterator = getNoRaidZoneEntries().listIterator(); iterator.hasNext(); ) {
            gravemindData.put("no_raid_zone_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        nbt.put("gravemindData", gravemindData);

        RaidData.save(nbt);
        StatisticsData.save(nbt);

        return nbt;
    }

    /**
     * Accessors
     **/

    public boolean isRaidCooldownOver() {
        return getTicksSinceLastRaid() >= TickUnits.convertMinutesToTicks(ModConfig.SERVER.sculk_raid_global_cooldown_between_raids_minutes.get());
    }

    public int getTicksSinceLastRaid() {
        setDirty();
        return ticksSinceLastRaid;
    }

    public void setTicksSinceLastRaid(int ticksSinceLastRaid) {
        this.ticksSinceLastRaid = ticksSinceLastRaid;
        setDirty();
    }

    public void incrementTicksSinceLastRaid() {
        this.ticksSinceLastRaid++;
        setDirty();
    }

    public boolean isSculkNodeCooldownOver() {
        return ticksSinceSculkNodeDestruction >= Gravemind.TICKS_BETWEEN_NODE_SPAWNS;
    }

    public int getTicksSinceSculkNodeDestruction() {
        setDirty();
        return ticksSinceSculkNodeDestruction;
    }

    public void setTicksSinceSculkNodeDestruction(int ticksSinceSculkNodeDestruction) {
        this.ticksSinceSculkNodeDestruction = ticksSinceSculkNodeDestruction;
        setDirty();
    }

    public void incrementTicksSinceSculkNodeDestruction() {
        this.ticksSinceSculkNodeDestruction++;
        setDirty();
    }

    public void resetTicksSinceSculkNodeDestruction() {
        //Send message to all players that node has spawned
        this.ticksSinceSculkNodeDestruction = 0;
        setDirty();
    }

    /**
     * Gets how much Sculk mass the Sculk horde has.
     *
     * @return An integer representing all Sculk mass accumulated.
     */
    public int getSculkAccumulatedMass() {
        setDirty();
        return sculkAccumulatedMass;
    }

    /**
     * Adds to the sculk accumulated mass
     *
     * @param amount The amount you want to add
     */
    public void addSculkAccumulatedMass(int amount) {
        setDirty();
        sculkAccumulatedMass += amount;
    }

    /**
     * Subtracts from the Sculk Accumulate Mass
     *
     * @param amount The amount to substract
     */
    public void subtractSculkAccumulatedMass(int amount) {
        setDirty();
        sculkAccumulatedMass -= amount;
    }

    /**
     * Sets the value of sculk accumulate mass.
     *
     * @param amount The amount to set it to.
     */
    public void setSculkAccumulatedMass(int amount) {
        setDirty();
        sculkAccumulatedMass = amount;
    }

    public ArrayList<NodeEntry> getNodeEntries() {
        return nodeEntries;
    }

    public ArrayList<BeeNestEntry> getBeeNestEntries() {
        return beeNestEntries;
    }

    public Map<String, HostileEntry> getHostileEntries() {
        return hostileEntries;
    }

    public ArrayList<PriorityBlockEntry> getPriorityBlockEntries() {
        return priorityBlockEntries;
    }

    public ArrayList<DeathAreaEntry> getDeathAreaEntries() {
        return deathAreaEntries;
    }

    public ArrayList<AreaofInterestEntry> getAreasOfInterestEntries() {
        return areasOfInterestEntries;
    }

    /**
     * Adds a position to the list if it does not already exist
     *
     * @param positionIn The Posoition to add
     */
    public void addNodeToMemory(BlockPos positionIn)
    {
        if (!isNodePositionInMemory(positionIn) && getNodeEntries() != null)
        {
            getNodeEntries().add(new NodeEntry(positionIn));
            setDirty();
        }
    }

    /**
     * Adds a position to the list if it does not already exist
     *
     * @param positionIn The Position to add
     */
    public void addBeeNestToMemory(BlockPos positionIn)
    {
        if (!isBeeNestPositionInMemory(positionIn) && getBeeNestEntries() != null)
        {
            getBeeNestEntries().add(new BeeNestEntry(positionIn));
            setDirty();
        }
        // TODO For some reason this continously gets called, find out why
        //else if(DEBUG_MODE) System.out.println("Attempted to Add Nest To Memory but failed.");
    }

    public void addHostileToMemory(LivingEntity entityIn)
    {
        if (entityIn == null || EntityAlgorithms.isSculkLivingEntity.test(entityIn) || entityIn instanceof Creeper)
        {
            return;
        }

        String identifier = entityIn.getType().toString();
        if (!identifier.isEmpty())
        {
            getHostileEntries().putIfAbsent(identifier, new HostileEntry(identifier));
            setDirty();
        }
    }

    public void addPriorityBlockToMemory(BlockPos positionIn) {

        // If the list is null, dont even try
        if (getPriorityBlockEntries() == null)
        {
            return;
        }

        // If the block is already in the list, dont add it again
        for (PriorityBlockEntry entry : getPriorityBlockEntries())
        {
            if (entry.position == positionIn)
            {
                return;
            }
        }

        int priority;

        // Determine the priority of the block
        if(level.getBlockState(positionIn).is(ModBlocks.BlockTags.SCULK_RAID_TARGET_HIGH_PRIORITY))
        {
            priority = 2;
        }
        else if(level.getBlockState(positionIn).is(ModBlocks.BlockTags.SCULK_RAID_TARGET_MEDIUM_PRIORITY))
        {
            priority = 1;
        }
        else if (level.getBlockState(positionIn).is(ModBlocks.BlockTags.SCULK_RAID_TARGET_LOW_PRIORITY))
        {
            priority = 0;
        }
        else
        {
            SculkHorde.LOGGER.warn("Attempted to add a block to the priority list that was not a priority block");
            return;
        }

        getPriorityBlockEntries().add(new PriorityBlockEntry(positionIn, priority));
        setDirty();

    }

    public void addDeathAreaToMemory(BlockPos positionIn)
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to add a death area to memory but the list was null");
            return;
        }

        // If already exists in memory, dont add it again
        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).position == positionIn)
            {
                return;
            }
        }

        SculkHorde.LOGGER.info("Adding Death Area at " + positionIn + " to memory");
        getDeathAreaEntries().add(new DeathAreaEntry(positionIn));
        setDirty();
    }

    public Optional<AreaofInterestEntry> addAreaOfInterestToMemory(BlockPos positionIn) {
        if(getAreasOfInterestEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to add an area of interest to memory but the list was null");
            return Optional.empty();
        }

        // If already exists in memory, dont add it again
        for(int i = 0; i < getAreasOfInterestEntries().size(); i++)
        {
            if(getAreasOfInterestEntries().get(i).position == positionIn || getAreasOfInterestEntries().get(i).position.closerThan(positionIn, 100))
            {
                return Optional.empty();
            }
        }

        SculkHorde.LOGGER.info("Adding Area of Interest at " + positionIn + " to memory");
        AreaofInterestEntry entry = new AreaofInterestEntry(positionIn);
        getAreasOfInterestEntries().add(entry);
        setDirty();
        return Optional.of(entry);
    }

    public void addNoRaidZoneToMemory(BlockPos positionIn) {
        if(getNoRaidZoneEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to add a no raid zone to memory but the list was null");
            return;
        }

        // If already exists in memory, dont add it again
        for(int i = 0; i < getNoRaidZoneEntries().size(); i++)
        {
            if(getNoRaidZoneEntries().get(i).position == positionIn || getNoRaidZoneEntries().get(i).position.closerThan(positionIn, 100))
            {
                return;
            }
        }

        SculkHorde.LOGGER.info("Adding No Raid Zone at " + positionIn + " to memory");
        getNoRaidZoneEntries().add(new NoRaidZoneEntry(positionIn, 1000, level.getGameTime(), TickUnits.convertMinutesToTicks(ModConfig.SERVER.sculk_raid_no_raid_zone_duration_minutes.get())));
        setDirty();
    }

    private Optional<DeathAreaEntry> getDeathAreaWithinRange(BlockPos positionIn, int range)
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to get a death area from memory but the list was null");
            return Optional.empty();
        }

        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).position.closerThan(positionIn, range))
            {
                return Optional.of(getDeathAreaEntries().get(i));
            }
        }
        return Optional.empty();
    }

    public Optional<DeathAreaEntry> getDeathAreaWithHighestDeaths()
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to get a death area from memory but the list was null");
            return Optional.empty();
        }

        int highestDeathCount = 0;
        DeathAreaEntry highestDeathArea = null;

        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).deathCount > highestDeathCount)
            {
                highestDeathCount = getDeathAreaEntries().get(i).deathCount;
                highestDeathArea = getDeathAreaEntries().get(i);
            }
        }

        if(highestDeathArea == null)
        {
            return Optional.empty();
        }

        return Optional.of(highestDeathArea);
    }

    /**
     * Will try to return an Area of Interest Entry that is not in a no raid zone.
     * @return Optional<AreaofInterestEntry> - The area of interest entry that is not in a no raid zone
     */
    public Optional<AreaofInterestEntry> getAreaOfInterestEntryNotInNoRaidZone()
    {
        if(getAreasOfInterestEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to get an area of interest from memory but the list was null");
            return null;
        }

        for(int i = 0; i < getAreasOfInterestEntries().size(); i++)
        {
            // If the area of interest is not in a no raid zone, return it
            if(!getAreasOfInterestEntries().get(i).isInNoRaidZone())
            {
                return Optional.of(getAreasOfInterestEntries().get(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Will verify all enties to see if they exist in the world.
     * If not, they will be removed. <br>
     * Gets called in {@link com.github.sculkhorde.util.ForgeEventSubscriber#WorldTickEvent}
     */
    public void validateNodeEntries() {
        long startTime = System.nanoTime();
        for (int index = 0; index < nodeEntries.size(); index++) {
            //TODO: Figure out if not being in the overworld can mess this up
            if (!getNodeEntries().get(index).isEntryValid(level)) {
                resetTicksSinceSculkNodeDestruction();
                getNodeEntries().remove(index);
                index--;
                setDirty();
            }
        }
        long endTime = System.nanoTime();
        if (SculkHorde.isDebugMode())
        {
            System.out.println("Node Validation Took " + TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) + " milliseconds");
        }
    }


    /**
     * Will verify all enties to see if they exist in the world.
     * Will also reasses the parentNode for each one.
     * If not, they will be removed. <br>
     * Gets called in {@link com.github.sculkhorde.util.ForgeEventSubscriber#WorldTickEvent}
     */
    public void validateBeeNestEntries() {
        long startTime = System.nanoTime();
        for (int index = 0; index < getBeeNestEntries().size(); index++) {
            getBeeNestEntries().get(index).setParentNodeToClosest();
            //TODO: Figure out if not being in the overworld can mess this up
            if (!getBeeNestEntries().get(index).isEntryValid(level)) {
                getBeeNestEntries().remove(index);
                index--;
                setDirty();
            }
        }
        long endTime = System.nanoTime();
        if (SculkHorde.isDebugMode())
        {
            System.out.println("Bee Nest Validation Took " + TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) + " milliseconds");
        }
    }

    public void validateAreasOfInterest()
    {
        long startTime = System.currentTimeMillis();
        for (int index = 0; index < getAreasOfInterestEntries().size(); index++)
        {
            if (!getAreasOfInterestEntries().get(index).isInNoRaidZone())
            {
                SculkHorde.LOGGER.info("Area of Interest at " + getAreasOfInterestEntries().get(index).position + " is on no raid zone. Removing from memory.");
                getAreasOfInterestEntries().remove(index);
                index--;
                setDirty();
            }
        }

        long endTime = System.currentTimeMillis();
        if(SculkHorde.isDebugMode()) {
            SculkHorde.LOGGER.info("Area Of Interest Validation Took " + (endTime - startTime) + " milliseconds");
        }
    }

    public void validateNoRaidZoneEntries()
    {
        long startTime = System.currentTimeMillis();
        for (int index = 0; index < getNoRaidZoneEntries().size(); index++)
        {
            if (getNoRaidZoneEntries().get(index).isExpired(level.getGameTime()))
            {
                SculkHorde.LOGGER.info("No Raid Zone Entry at " + getNoRaidZoneEntries().get(index).position + " has expired. Removing from memory.");
                getNoRaidZoneEntries().remove(index);
                index--;
                setDirty();
            }
        }

        long endTime = System.currentTimeMillis();
        if(SculkHorde.isDebugMode()) {
            SculkHorde.LOGGER.info("No Raid Zone Validation Took " + (endTime - startTime) + " milliseconds");
        }
    }

    /**
     * Will check the positons of all entries to see
     * if they match the parameter.
     *
     * @param position The position to cross reference
     * @return true if in memory, false otherwise
     */
    public boolean isBeeNestPositionInMemory(BlockPos position) {
        for (BeeNestEntry entry : getBeeNestEntries()) {
            if (entry.position == position) {
                return true;
            }
        }
        return false;
    }


    /**
     * Will check the positons of all entries to see
     * if they match the parameter.
     *
     * @param position The position to cross reference
     * @return true if in memory, false otherwise
     */
    public boolean isNodePositionInMemory(BlockPos position) {
        for (NodeEntry entry : getNodeEntries()) {
            if (entry.position.equals(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of known node positions
     *
     * @return The Closest TreeNode
     */
    public NodeEntry getClosestNodeEntry(BlockPos pos) {
        NodeEntry closestNode = null;
        double closestDistance = Double.MAX_VALUE;
        for (NodeEntry node : getNodeEntries()) {
            if (pos.distSqr(node.position) < closestDistance) {
                closestNode = node;
                closestDistance = pos.distSqr(node.position);
            }
        }
        return closestNode;
    }


    public boolean isInRangeOfNode(BlockPos pos, int distance) {

        if(getBlockDistanceXZ(BlockPos.ZERO, pos) > distance)
        {
            return false;
        }

        if (getNodeEntries().isEmpty()) {
            return false;
        }

        return getBlockDistanceXZ(getClosestNodeEntry(pos).position, pos) <= distance;

    }

    public void removeNodeFromMemory(BlockPos positionIn)
    {
        if(getNodeEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to remove an area of interest from memory but the list was null");
            return;
        }

        for(int i = 0; i < getNodeEntries().size(); i++)
        {
            if(getNodeEntries().get(i).position.equals(positionIn))
            {
                getNodeEntries().remove(i);
                setDirty();
                resetTicksSinceSculkNodeDestruction();
                return;
            }
        }
        setDirty();
    }

    public void removeDeathAreaFromMemory(BlockPos positionIn)
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to remove a death area from memory but the list was null");
            return;
        }

        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).position == positionIn)
            {
                getDeathAreaEntries().remove(i);
                setDirty();
                return;
            }
        }
        setDirty();
    }

    public void removeAreaOfInterestFromMemory(BlockPos positionIn)
    {
        if(getAreasOfInterestEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to remove an area of interest from memory but the list was null");
            return;
        }

        for(int i = 0; i < getAreasOfInterestEntries().size(); i++)
        {
            if(getAreasOfInterestEntries().get(i).position == positionIn)
            {
                getAreasOfInterestEntries().remove(i);
                setDirty();
                return;
            }
        }
        setDirty();
    }

    /**
     * This method gets called every time a sculk mob dies.
     * We check if the death happened in a death area.
     * If it did, we iterate the death count of that area.
     * If it did not, we create a new death area.
     *
     * @param deathPosition The position where the player died
     */
    public void reportDeath(BlockPos deathPosition)
    {
        // If a death area already exist close to this location, iterate the death count
        Optional<DeathAreaEntry> deathArea = getDeathAreaWithinRange(deathPosition, 100);
        if(deathArea.isPresent())
        {
            deathArea.get().iterateDeathCount();
            setDirty();
            return;
        }

        // If the death area does not exist, create a new one
        addDeathAreaToMemory(deathPosition);
    }

    public static class PriorityBlockEntry
    {
        private final BlockPos position;
        private final int priority;

        public PriorityBlockEntry(BlockPos positionIn, int priorityIn)
        {
            position = positionIn;
            priority = priorityIn;
        }

        public BlockPos getPosition()
        {
            return position;
        }

        public int getPriority()
        {
            return priority;
        }

        public boolean isEntryValid(ServerLevel level)
        {
            BlockState blockState = level.getBlockState(position);

            if (blockState.is(ModBlocks.BlockTags.SCULK_RAID_TARGET_HIGH_PRIORITY) && priority == 2)
            {
                return true;
            }
            else if (blockState.is(ModBlocks.BlockTags.SCULK_RAID_TARGET_MEDIUM_PRIORITY) && priority == 1)
            {
                return true;
            }
            else if (blockState.is(ModBlocks.BlockTags.SCULK_RAID_TARGET_LOW_PRIORITY) && priority == 0)
            {
                return true;
            }

            return false;

        }


        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putLong("priority", priority);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static PriorityBlockEntry serialize(CompoundTag nbt)
        {
            return new PriorityBlockEntry(BlockPos.of(nbt.getLong("position")), nbt.getInt("priority"));
        }


    }

    /**
     * This class is a representation of the actual
     * Sculk Nodes in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    public static class NodeEntry
    {
        private final BlockPos position; //The Location in the world where the node is
        private long lastTimeWasActive;

        private long activationTimeStamp;

        private boolean IsActive;


        /**
         * Default Constructor
         * @param positionIn The physical location
         */
        public NodeEntry(BlockPos positionIn)
        {
            position = positionIn;
            lastTimeWasActive = SculkHorde.savedData.level.getGameTime();
        }

        public BlockPos getPosition()
        {
            return position;
        }

        public boolean isActive()
        {
            return IsActive;
        }

        public void setActive(boolean activeIn)
        {
            IsActive = activeIn;
            SculkNodeBlockEntity sculkNodeBlockEntity = (SculkNodeBlockEntity) SculkHorde.savedData.level.getBlockEntity(position);
            sculkNodeBlockEntity.setActive(activeIn);
        }

        public long getLastTimeWasActive()
        {
            return lastTimeWasActive;
        }

        public void setLastTimeWasActive(long lastTimeWasActiveIn)
        {
            lastTimeWasActive = lastTimeWasActiveIn;
        }

        public void setActivationTimeStamp(long activationTimeStampIn)
        {
            activationTimeStamp = activationTimeStampIn;
        }

        public long getActivationTimeStamp()
        {
            return activationTimeStamp;
        }

        /**
         * Checks the world to see if the node is still there.
         * @param worldIn The world to check
         * @return True if in the world at location, false otherwise
         */
        public boolean isEntryValid(ServerLevel worldIn)
        {
            return worldIn.getBlockState(position).getBlock().equals(ModBlocks.SCULK_NODE_BLOCK.get());
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putLong("lastTimeWasActive", lastTimeWasActive);
            nbt.putLong("activationTimeStamp", activationTimeStamp);
            nbt.putBoolean("IsActive", IsActive);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static NodeEntry serialize(CompoundTag nbt)
        {
            NodeEntry entry = new NodeEntry(BlockPos.of(nbt.getLong("position")));
            entry.setLastTimeWasActive(nbt.getLong("lastTimeWasActive"));
            entry.setActivationTimeStamp(nbt.getLong("activationTimeStamp"));
            entry.setActive(nbt.getBoolean("IsActive"));
            return entry;
        }

    }

    /**
     * This class is a representation of the actual
     * Bee Nests in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    public static class BeeNestEntry
    {
        private final BlockPos position; //The location in the world where the node is
        private BlockPos parentNodePosition; //The location of the Sculk TreeNode that this Nest belongs to

        /**
         * Default Constructor
         * @param positionIn The Position of this Nest
         */
        public BeeNestEntry(BlockPos positionIn)
        {
            position = positionIn;
        }

        public BeeNestEntry(BlockPos positionIn, BlockPos parentPositionIn)
        {
            position = positionIn;
            parentNodePosition = parentPositionIn;
        }

        /**
         * Checks if the block does still exist in the world.
         * @param worldIn The world to check
         * @return True if valid, false otherwise.
         */
        public boolean isEntryValid(ServerLevel worldIn)
        {
            return worldIn.getBlockState(position).getBlock().equals(ModBlocks.SCULK_BEE_NEST_BLOCK.get());
        }


        /**
         * is Hive enabled?
         * @return True if enabled, false otherwise
         */
        public boolean isOccupantsExistingDisabled(ServerLevel worldIn)
        {
            return SculkBeeNestBlock.isNestClosed(worldIn.getBlockState(position));
        }

        /**
         * Sets Hive to deny bees leaving
         */
        public void disableOccupantsExiting(ServerLevel world)
        {
            SculkBeeNestBlock.setNestClosed(world, world.getBlockState(position), position);
        }


        /**
         * Sets Hive to allow bees leaving
         */
        public void enableOccupantsExiting(ServerLevel world)
        {
            SculkBeeNestBlock.setNestOpen(world, world.getBlockState(position), position);
        }


        public NodeEntry getClosestNode(BlockPos pos)
        {
            NodeEntry closestEntry = getGravemindMemory().getNodeEntries().get(0);
            for(NodeEntry entry : getGravemindMemory().getNodeEntries())
            {
                //If entry is closer than our current closest entry
                if(getBlockDistance(pos, entry.position) < getBlockDistance(pos, closestEntry.position))
                {
                    closestEntry = entry;
                }
            }
            return closestEntry;
        }

        /**
         * Checks list of node entries and finds the closest one.
         * It then sets the parentNodePosition to be the position of
         * the closest node.
         */
        public void setParentNodeToClosest()
        {
            //Make sure nodeEntries isnt null and nodeEntries isnt empty
            if(getGravemindMemory().getNodeEntries() != null && !getGravemindMemory().getNodeEntries().isEmpty())
            {
                NodeEntry closestEntry = getGravemindMemory().getNodeEntries().get(0);
                for(NodeEntry entry : getGravemindMemory().getNodeEntries())
                {
                    //If entry is closer than our current closest entry
                    if(getBlockDistance(position, entry.position) < getBlockDistance(position, closestEntry.position))
                    {
                        closestEntry = entry;
                    }
                }
                parentNodePosition = closestEntry.position;
            }
        }


        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            if(parentNodePosition != null) nbt.putLong("parentNodePosition", parentNodePosition.asLong());
            return nbt;
        }


        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static BeeNestEntry serialize(CompoundTag nbt)
        {
            return new BeeNestEntry(BlockPos.of(nbt.getLong("position")), BlockPos.of(nbt.getLong("parentNodePosition")));
        }
    }

    /**
     * This class is a representation of the actual
     * Sculk Nodes in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    private static class HostileEntry
    {
        private final String identifier; //The String that is the class name identifier of the mob. Example: class net.minecraft.entity.monster.SpiderEntity

        /**
         * Default Constructor
         * @param identifierIn The String that is the class name identifier of the mob. <br>
         * Example: class net.minecraft.entity.monster.SpiderEntity
         */
        public HostileEntry(String identifierIn)
        {
            identifier = identifierIn;
        }


        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("identifier", identifier);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static HostileEntry serialize(CompoundTag nbt)
        {
            return new HostileEntry(nbt.getString("identifier"));
        }

    }

    public static class DeathAreaEntry
    {
        private final BlockPos position; // The Location of the Death Area
        private int deathCount; // The number of deaths that have occurred in this area

        public DeathAreaEntry(BlockPos positionIn)
        {
            position = positionIn;
            deathCount = 1;
        }

        public DeathAreaEntry(BlockPos positionIn, int deathCountIn)
        {
            position = positionIn;
            deathCount = deathCountIn;
        }

        public void setDeathCount(int deathCountIn)
        {
            deathCount = deathCountIn;
        }

        public int getDeathCount()
        {
            return deathCount;
        }

        public void iterateDeathCount()
        {
            deathCount++;
        }

        public BlockPos getPosition()
        {
            return position;
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putInt("deathCount", deathCount);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static DeathAreaEntry serialize(CompoundTag nbt) {
            return new DeathAreaEntry(BlockPos.of(nbt.getLong("position")), nbt.getInt("deathCount"));
        }
    }

    public static class AreaofInterestEntry
    {
        private final BlockPos position; // The Location of the Death Area
        private long ticksSinceLastRaid;

        public AreaofInterestEntry(BlockPos positionIn)
        {
            position = positionIn;
        }

        public AreaofInterestEntry(BlockPos positionIn, long ticksSinceLastRaidIn)
        {
            position = positionIn;
            ticksSinceLastRaid = ticksSinceLastRaidIn;
        }

        public BlockPos getPosition()
        {
            return position;
        }

        public boolean isInNoRaidZone()
        {
            for(NoRaidZoneEntry entry : SculkHorde.savedData.getNoRaidZoneEntries())
            {
                if(entry.isBlockPosInRadius(getPosition()))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putLong("ticksSinceLastRaid", ticksSinceLastRaid);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static AreaofInterestEntry serialize(CompoundTag nbt) {
            return new AreaofInterestEntry(BlockPos.of(nbt.getLong("position")), nbt.getLong("ticksSinceLastRaid"));
        }
    }

    public static class NoRaidZoneEntry
    {
        private final BlockPos position; // The Location
        private final int radius;
        private final long timeOfCreation; // this.level.getGameTime();
        private long durationInTicksUntilExpiration;

        public NoRaidZoneEntry(BlockPos positionIn, int radiusIn, long gameTimeStampIn, long durationUntilExpirationIn)
        {
            position = positionIn;
            radius = radiusIn;
            timeOfCreation = gameTimeStampIn;
            durationInTicksUntilExpiration = durationUntilExpirationIn;
        }


        public BlockPos getPosition()
        {
            return position;
        }

        public int getRadius()
        {
            return radius;
        }

        public long getTimeOfCreation()
        {
            return timeOfCreation;
        }

        public long getDurationInTicksUntilExpiration()
        {
            return durationInTicksUntilExpiration;
        }

        public boolean isExpired(long currentTimeStamp)
        {
            long defaultTicksUntilExpiration = TickUnits.convertMinutesToTicks(ModConfig.SERVER.sculk_raid_no_raid_zone_duration_minutes.get());
            long ticksUntilThisNoRaidZoneExpires = getDurationInTicksUntilExpiration();
            // If the user has set a lower duration in the config, we will use that instead
            if(ticksUntilThisNoRaidZoneExpires > defaultTicksUntilExpiration)
            {
                durationInTicksUntilExpiration = defaultTicksUntilExpiration;
            }

            return (currentTimeStamp - getTimeOfCreation()) > getDurationInTicksUntilExpiration();
        }

        public boolean isBlockPosInRadius(BlockPos blockPosIn)
        {
            return position.closerThan(blockPosIn, radius);
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putInt("radius", radius);
            nbt.putLong("gameTimeStamp", timeOfCreation);
            nbt.putLong("durationUntilExpiration", durationInTicksUntilExpiration);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static NoRaidZoneEntry serialize(CompoundTag nbt)
        {
            return new NoRaidZoneEntry(BlockPos.of(nbt.getLong("position")), nbt.getInt("radius"), nbt.getLong("gameTimeStamp"), nbt.getLong("durationUntilExpiration"));
        }
    }
}
