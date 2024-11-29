package com.github.sculkhorde.util.old;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class SphereEntityChecker {

    /**
     * Checks all tracked entities to see if they have left the sphere.
     *
     * @param world  The ServerLevel instance.
     * @param center The center of the sphere.
     * @param radius The radius of the sphere.
     */
    public static void checkEntities(ServerLevel world, BlockPos center, int radius) {
        double radiusSquared = radius * radius;
        Set<Entity> entitiesToRemove = new HashSet<>();

        for (Entity entity : SphereEntityTracker.getTrackedEntities()) {
            if (!entity.isAlive()) {
                // Remove dead entities from tracking
                entitiesToRemove.add(entity);
                continue;
            }

            double distanceSquared = entity.position().distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSquared > radiusSquared) {
                // Teleport entity back inside the sphere
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

                entity.teleportTo(safePosition.x, safePosition.y, safePosition.z);
                System.out.println("Teleported entity " + entity.getName().getString() + " back into the sphere.");
            }
        }

        // Remove invalid entities from tracking
        SphereEntityTracker.getTrackedEntities().removeAll(entitiesToRemove);
    }
}

