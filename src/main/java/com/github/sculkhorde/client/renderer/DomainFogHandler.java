package com.github.sculkhorde.client.renderer;

import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.domains.SculkDomain;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent.RenderFog;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

public class DomainFogHandler {

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onRenderFog(RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        // Check if the player is within the sphere
        Vec3 cameraPos = camera.getPosition();

        Map<Integer, SculkDomain> trackedDomains = SculkHorde.sculkDomainHandler.getTrackedDomains();
        for (Map.Entry<Integer, SculkDomain> entry : trackedDomains.entrySet()) {

            SculkDomain domain = entry.getValue();

            BlockPos blockCenter = domain.getCenter();
            Vec3 vec3Center = new Vec3(blockCenter.getX()+0.5, blockCenter.getY()+0.5, blockCenter.getZ()+0.5);

            int radius = domain.getRadius();    // 20 Blocks
            float innerDome = radius*radius;    // 400 Blocks
            float outerDome = domain.fog.externalFogRadius*domain.fog.externalFogRadius; // 1600 Blocks

            double distanceSquared = cameraPos.distanceToSqr(vec3Center);

            float red = domain.fog.red;
            float green = domain.fog.green;
            float blue = domain.fog.blue;

            float distanceToInner = (float) (distanceSquared-innerDome);
            float fogDistance = Mth.sqrt(distanceToInner);

            if (distanceSquared <= innerDome) {
                applyCustomFog(event, red, green, blue, 0.0f, radius); // Fog color, start, and end distances
            } else if (distanceSquared <= outerDome){
                applyCustomFog(event, red, green, blue, fogDistance, fogDistance+2); // Fog color, start, and end distances
            }
        }
    }

    private void applyCustomFog(RenderFog event, float red, float green, float blue, float startDistance, float endDistance) {
        // Set fog start and end distances
        RenderSystem.setShaderFogStart(startDistance);
        RenderSystem.setShaderFogEnd(endDistance);
        RenderSystem.setShaderFogColor(red, green, blue);
    }
}

/*
package com.github.sculkhorde.client.renderer;

import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.domains.SculkDomain;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent.RenderFog;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

public class FogEffectHandler {

    private static final double SPHERE_RADIUS = 10.0; // Example radius
    private static final Vec3 SPHERE_CENTER = new Vec3(100.5, 64.5, 100.5); // Sphere center (example)

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Vec3 playerPos = player.position();

        // Check if the player is in any sphere
        boolean isPlayerInSphere = false;
        Map<Integer, SculkDomain> trackedDomains = SculkHorde.sculkDomainTracker.getTrackedDomains();
        for (Map.Entry<Integer, SculkDomain> entry : trackedDomains.entrySet()) {
            SculkDomain domain = entry.getValue();
            BlockPos blockCenter = domain.getCenter();
            Vec3 vec3Center = new Vec3(blockCenter.getX()+0.5, blockCenter.getY()+0.5, blockCenter.getZ()+0.5);

            if (playerPos.distanceToSqr(vec3Center) <= domain.getRadius() * domain.getRadius()) {
                isPlayerInSphere = true;
                return; // Exit early if player is inside a sphere
            }
        }
    }

    @SubscribeEvent
    public void onRenderFog(RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        // Check if the player is within the sphere
        Vec3 cameraPos = camera.getPosition();

        Map<Integer, SculkDomain> trackedDomains = SculkHorde.sculkDomainTracker.getTrackedDomains();
        for (Map.Entry<Integer, SculkDomain> entry : trackedDomains.entrySet()) {

            BlockPos blockCenter = entry.getValue().getCenter();
            Vec3 vec3Center = new Vec3(blockCenter.getX()+0.5, blockCenter.getY()+0.5, blockCenter.getZ()+0.5);

            double distanceSquared = cameraPos.distanceToSqr(vec3Center);
            if (distanceSquared <= SPHERE_RADIUS * SPHERE_RADIUS) {
                // Apply custom fog
                applyCustomFog(event, 0.5f, 0.7f, 0.8f, 5.0f, 10.0f); // Fog color, start, and end distances
            }
        }


        double distanceSquared = cameraPos.distanceToSqr(SPHERE_CENTER);
        if (distanceSquared <= SPHERE_RADIUS * SPHERE_RADIUS) {
            // Apply custom fog
            applyCustomFog(event, 0.5f, 0.7f, 0.8f, 5.0f, 10.0f); // Fog color, start, and end distances
            return;
        }
    }

    private void applyCustomFog(RenderFog event, float red, float green, float blue, float startDistance, float endDistance) {
        // Set fog start and end distances
        RenderSystem.setShaderFogStart(startDistance);
        RenderSystem.setShaderFogEnd(endDistance);
        RenderSystem.setShaderFogColor(red, green, blue);

        // Cancel default fog application
        event.setCanceled(true);
    }
}




 */



