package com.github.sculkhorde.util.old;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class SphereEntityTracker {

    private static final Set<Entity> trackedEntities = new HashSet<>();

    /**
     * Find and track all entities within a sphere.
     *
     * @param world  The ServerLevel instance.
     * @param center The center of the sphere.
     * @param radius The radius of the sphere.
     */
    public static void trackEntitiesInSphere(ServerLevel world, BlockPos center, int radius) {
        double radiusSquared = radius * radius;

        // Loop through entities within the chunk bounds
        List<Entity> entities = world.getEntities(null,
                new net.minecraft.world.phys.AABB(
                        center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                        center.getX() + radius, center.getY() + radius, center.getZ() + radius));

        // Filter entities that are inside the sphere and track them
        for (Entity entity : entities) {
            double distanceSquared = entity.position().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSquared <= radiusSquared) {
                trackedEntities.add(entity);
            }
        }

        System.out.println("Tracking " + trackedEntities.size() + " entities within the sphere.");
    }

    public static void untrackEntitiesInSphere(ServerLevel world, BlockPos center, int radius) {
        double radiusSquared = radius * radius;

        // Loop through entities within the chunk bounds
        List<Entity> entities = world.getEntities(null,
                new net.minecraft.world.phys.AABB(
                        center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                        center.getX() + radius, center.getY() + radius, center.getZ() + radius));

        // Filter entities that are inside the sphere and track them
        for (Entity entity : entities) {
            double distanceSquared = entity.position().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSquared <= radiusSquared) {
                trackedEntities.remove(entity);
            }
        }

        System.out.println("Tracking " + trackedEntities.size() + " entities within the sphere.");
    }

    /**
     * Returns the set of tracked entities.
     */
    public static Set<Entity> getTrackedEntities() {
        return trackedEntities;
    }
}

