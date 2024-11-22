package com.github.sculkhorde.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class EmptyRenderer extends EntityRenderer<Entity> {
    public EmptyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
    }

    @Override
    public ResourceLocation getTextureLocation(Entity p_114482_) {
        return null;
    }
}
