package com.github.sculkhorde.systems.domains;

import com.github.sculkhorde.common.entity.infection.CursorSurfaceInfectorEntity;
import com.github.sculkhorde.common.entity.infection.CursorSurfacePurifierEntity;
import com.github.sculkhorde.core.*;
import com.github.sculkhorde.util.TaskScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class SculkDomain {
    private final ServerLevel serverLevel;
    private final BlockPos center;
    private final int radius;

    private final boolean breakableBarrier;
    private final boolean regenBarrier;

    private boolean regenEnabled = false;
    private int regenCheckCooldown = 10;
    private final int id;

    private final Map<BlockPos, BlockState> replacedMap = new HashMap<>();
    private final Set<Entity> trackedEntities = new HashSet<>();

    private final Random r = new Random();

    // [Fog Handler Variables] -------------------------------------------------------------------------------------------------------------------------------------------- //
    public class Fog {
        public boolean renderInDomain = true;       // Should display foggy effect to players within a domain
        public boolean renderOutsideDomain = true;  // Should display foggy effect to players within a domain
        public int externalFogRadius;               // Secondary domain fog that gets denser closer player is to center when outside domain

        public float start = 0.0f;          // If more than 0f, the fog will not begin at the camera of the player
        public float end = 16.0f;           // Distance at which fog becomes completely opaque
        public float red = 0.071f;          // Red   | RGB Colour of Fog - Max Value = 1.0f
        public float green = 0.118f;        // Green | RGB Colour of Fog - Max Value = 1.0f
        public float blue = 0.188f;         // Blue  | RGB Colour of Fog - Max Value = 1.0f
    }

    public final Fog fog = new Fog();   // Rather than using an extra 5 - 9 extra functions, this seemed like the cleaner method
    // [Fog Handler Variables] -------------------------------------------------------------------------------------------------------------------------------------------- //

    public SculkDomain(ServerLevel level, BlockPos centerBlock, int sphereRadius, boolean breakable, boolean regen, int domainId) {
        serverLevel = level;
        center = centerBlock;
        radius = sphereRadius;

        fog.externalFogRadius = radius*2;

        breakableBarrier = breakable;
        regenBarrier = regen;
        id = domainId;

        SculkHorde.LOGGER.info("Created new domain with ID: " + id);
    }

    public int getId() {
        return id;
    }

    public Set<Entity> getTrackedEntities() {
        return trackedEntities;
    }

    public void domainShatter() {
        for (Entity entity : trackedEntities) {
            if (entity instanceof LivingEntity livingEntity) {
                MobEffectInstance darkness = new MobEffectInstance(MobEffects.DARKNESS, 200, 2, false, false);
                livingEntity.addEffect(darkness);
            }
        }

        /*
        for (Map.Entry<BlockPos, BlockState> entry : replacedMap.entrySet()) {
            serverLevel.setBlock(entry.getKey(), entry.getValue(), 3);
        }
         */

        new SculkDomainBuilder(this, serverLevel, replacedMap);

        //serverLevel.playSound(null, center, ModSounds.NODE_DESTROY_SOUND.get(), SoundSource.HOSTILE, 0.5f, 0.5f);
    }

    public void domainExpansion() {
        domainCheckEntities();
        for (Entity entity : trackedEntities) {
            if (entity instanceof LivingEntity livingEntity) {
                MobEffectInstance darkness = new MobEffectInstance(MobEffects.DARKNESS, 20+(radius*2), 4, false, false);
                MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20+(radius*2), 6, false, false);
                livingEntity.addEffect(darkness);
                livingEntity.addEffect(slowness);
            }
        }
        newSphere();
    }

    public void domainCheckEntities() {
        getEntitiesInDomain();
        checkEntities();
        if (regenCheckCooldown <= 0) {
            regenSphere();
        } else {
            regenCheckCooldown--;
        }
    }

    public void domainEjectEntity(Entity entity) {
        trackedEntities.remove(entity);
        entity.teleportTo(center.getX(), center.getY() + radius + 2, center.getZ());
    }

    public int getRadius() {
        return radius;
    }

    public BlockPos getCenter() {
        return center;
    }

    public void domainCompleted() {
        regenEnabled = true;
    }

    public void domainRegenerated() {
        regenEnabled = true;
    }

    public void domainDissolved() {

    }

    private void checkEntities() {
        double radiusSquared = radius * radius;
        Set<Entity> entitiesToRemove = new HashSet<>();

        for (Entity entity : trackedEntities) {
            if ((entity instanceof CursorSurfacePurifierEntity) || (entity instanceof PrimedTnt)) {
                entity.discard();
            }

            if (entity instanceof ItemEntity item) {
                if (ModConfig.SERVER.isItemEdibleToCursors(item)) {
                    int massToAdd = ((ItemEntity)entity).getItem().getCount();
                    SculkHorde.savedData.addSculkAccumulatedMass(massToAdd);
                    SculkHorde.statisticsData.addTotalMassFromInfestedCursorItemEating(massToAdd);
                    item.discard();
                }
            }

            if (!entity.isAlive()) {
                // Remove dead entities from tracking
                entitiesToRemove.add(entity);
                continue;
            }

            double distanceSquared = entity.position().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSquared > radiusSquared) {
                // Teleport entity back inside the sphere

                /*
                Vec3 directionToCenter = new Vec3(
                        center.getX() + 0.5 - entity.getX(),
                        center.getY() + 0.5 - entity.getY(),
                        center.getZ() + 0.5 - entity.getZ()
                ).normalize();

                Vec3 safePosition = new Vec3(
                        center.getX() + 0.5 - directionToCenter.x * (radius - 1),
                        center.getY() + 0.5 - directionToCenter.y * (radius - 1),
                        center.getZ() + 0.5 - directionToCenter.z * (radius - 1)
                );
                 */

                if (entity instanceof LivingEntity livingEntity) {
                    MobEffectInstance darkness = new MobEffectInstance(MobEffects.DARKNESS, 20, 4, false, false);
                    livingEntity.addEffect(darkness);
                }
                entity.teleportTo(center.getX(), center.getY()+1, center.getZ());
                serverLevel.playSound(null, center, ModSounds.SCULK_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1f, 1f);
            }
        }

        // Remove invalid entities from tracking
        trackedEntities.removeAll(entitiesToRemove);
    }

    private void getEntitiesInDomain() {
        double radiusSquared = radius * radius;

        // Loop through entities within the chunk bounds
        List<Entity> entities = serverLevel.getEntities(null,
                new net.minecraft.world.phys.AABB(
                        center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                        center.getX() + radius, center.getY() + radius, center.getZ() + radius));

        // Filter entities that are inside the sphere and track them
        for (Entity entity : entities) {
            double distanceSquared = entity.position().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSquared < radiusSquared && !(entity instanceof CursorSurfaceInfectorEntity)) {
                trackedEntities.add(entity);
            }
        }
    }

    private final Block clearBarrier = ModBlocks.BARRIER_OF_SCULK.get();
    private final Block solidBarrier = ModBlocks.SOLID_BARRIER_OF_SCULK.get();
    private final Block breakableClearBarrier = ModBlocks.BREAKABLE_BARRIER_OF_SCULK.get();
    private final Block breakableSolidBarrier = ModBlocks.BREAKABLE_SOLID_BARRIER_OF_SCULK.get();

    private void regenSphere() {
        if (regenEnabled) {
            SculkHorde.LOGGER.info(this + ": Running Regen Check...");
            ArrayList<BlockPos> blocksToRegen = new ArrayList<>();

            for (Map.Entry<BlockPos, BlockState> entry : replacedMap.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState blockAtPos = serverLevel.getBlockState(pos);

                if (!blockAtPos.is(clearBarrier) && !blockAtPos.is(solidBarrier) && !blockAtPos.is(breakableClearBarrier) && !blockAtPos.is(breakableSolidBarrier)) {
                    blocksToRegen.add(pos);
                }
            }

            if (blocksToRegen.isEmpty()) {
                regenCheckCooldown = 5;
                SculkHorde.LOGGER.info(this + ": Nothing to regen, rescheduling...");
            }
            else {
                SculkHorde.LOGGER.info(this + ": Blocks found to regen, scheduling task...");
                Collections.shuffle(blocksToRegen);
                TaskScheduler.scheduleTask(100, () -> new SculkDomainBuilder(this, serverLevel, blocksToRegen, breakableBarrier));
                regenEnabled = false;
                regenCheckCooldown = 5;
            }
        }
    }

    private void newSphere() {
        ArrayList<BlockPos> blocksInside = new ArrayList<>();
        ArrayList<BlockPos> barrierBlocks = new ArrayList<>();

        // Loop over all positions within a cube that bounds the sphere
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distanceSquared = x * x + y * y + z * z;
                    double outerRadiusSquared = radius * radius;
                    double innerRadiusSquared = (radius - 1) * (radius - 1);

                    BlockPos pos = center.offset(x, y, z);

                    // Check if this position is on the shell of the sphere
                    if (distanceSquared <= innerRadiusSquared) {
                        if (!serverLevel.getBlockState(pos).equals(Blocks.AIR.defaultBlockState())) {
                            blocksInside.add(pos);
                        }
                    }
                    else if (distanceSquared <= outerRadiusSquared /*&& distanceSquared > innerRadiusSquared*/) {

                        Block state = serverLevel.getBlockState(pos).getBlock();

                        // Store the current block state if replacing
                        if (!state.equals(ModBlocks.BARRIER_OF_SCULK.get()) && !state.equals(ModBlocks.SOLID_BARRIER_OF_SCULK.get())) {
                            replacedMap.put(pos, serverLevel.getBlockState(pos));
                        }

                        barrierBlocks.add(pos);
                    }
                }
            }
        }

        new SculkDomainBuilder(this, serverLevel, barrierBlocks, blocksInside, breakableBarrier);
    }

}
