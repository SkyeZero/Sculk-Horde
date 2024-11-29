package com.github.sculkhorde.client;

import com.github.sculkhorde.client.particle.SculkCrustParticle;
import com.github.sculkhorde.client.renderer.DomainFogHandler;
import com.github.sculkhorde.client.renderer.block.SculkSummonerBlockRenderer;
import com.github.sculkhorde.client.renderer.block.SoulHarvesterBlockRenderer;
import com.github.sculkhorde.client.renderer.entity.*;
import com.github.sculkhorde.common.screen.SoulHarvesterScreen;
import com.github.sculkhorde.core.*;
import com.google.common.collect.Maps;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

@Mod.EventBusSubscriber(modid = SculkHorde.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEventSubscriber {

    public final Map<EntityType<?>, EntityRenderer<?>> renderers = Maps.newHashMap();

    public <T extends Entity> void register(EntityType<T> p_229087_1_, EntityRenderer<? super T> p_229087_2_) {
        this.renderers.put(p_229087_1_, p_229087_2_);
    }

    @OnlyIn(Dist.CLIENT)
    public static void init(final FMLClientSetupEvent event) {
        // Register any client-specific handlers
        MinecraftForge.EVENT_BUS.register(new DomainFogHandler());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerRenders(final EntityRenderersEvent.RegisterRenderers event) {

        ItemBlockRenderTypes.setRenderLayer(ModBlocks.BARRIER_OF_SCULK.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.BREAKABLE_BARRIER_OF_SCULK.get(), RenderType.translucent());

        // Register Renderers for Entities

        event.registerEntityRenderer(ModEntities.SCULK_ZOMBIE.get(), SculkZombieRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_MITE.get(), SculkMiteRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_MITE_AGGRESSOR.get(), SculkMiteAggressorRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_SPITTER.get(), SculkSpitterRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_BEE_INFECTOR.get(), SculkBeeInfectorRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_BEE_HARVESTER.get(), SculkBeeHarvesterRenderer::new);

        event.registerEntityRenderer(ModEntities.CUSTOM_ITEM_PROJECTILE_ENTITY.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_ACIDIC_PROJECTILE_ENTITY.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.PURIFICATION_FLASK_PROJECTILE_ENTITY.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_HATCHER.get(), SculkHatcherRenderer::new);

        event.registerEntityRenderer(ModEntities.CURSOR_PROBER.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.CURSOR_PURIFIER_PROBER.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.CURSOR_BRIDGER.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.CURSOR_SURFACE_INFECTOR.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.CURSOR_SURFACE_PURIFIER.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.CURSOR_TOP_DOWN_INFECTOR.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.CURSOR_TOP_DOWN_PURIFIER.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_SPORE_SPEWER.get(), SculkSporeSpewerRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_RAVAGER.get(), SculkRavagerRenderer::new);

        event.registerEntityRenderer(ModEntities.INFESTATION_PURIFIER.get(), InfestationPurifierRenderer::new);

        event.registerBlockEntityRenderer(ModBlockEntities.SCULK_SUMMONER_BLOCK_ENTITY.get(), context -> new SculkSummonerBlockRenderer());

        event.registerBlockEntityRenderer(ModBlockEntities.SOUL_HARVESTER_BLOCK_ENTITY.get(), context -> new SoulHarvesterBlockRenderer());

        event.registerEntityRenderer(ModEntities.SCULK_VINDICATOR.get(), SculkVindicatorRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_CREEPER.get(), SculkCreeperRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_ENDERMAN.get(), SculkEndermanRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_PHANTOM.get(), SculkPhantomRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_PHANTOM_CORPSE.get(), SculkPhantomCorpseRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_SALMON.get(), SculkSalmonRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_SQUID.get(), SculkSquidRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_PUFFERFISH.get(), SculkPufferfishRenderer::new);

        event.registerEntityRenderer(ModEntities.ENDER_BUBBLE_ATTACK.get(), EnderBubbleAttackRenderer::new);

        event.registerEntityRenderer(ModEntities.CHAOS_TELEPORATION_RIFT.get(), ChaosTeleporationRiftRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_SPINE_SPIKE_ATTACK.get(), SculkSpineSpikeAttackRenderer::new);

        event.registerEntityRenderer(ModEntities.AREA_EFFECT_SPHERICAL_CLOUD.get(), EmptyRenderer::new);

        event.registerEntityRenderer(ModEntities.SCULK_WITCH.get(), SculkWitchRenderer::new);

        //event.registerEntityRenderer(ModEntities.SCULK_SOUL_REAPER.get(), SculkSoulReaperRenderer::new);

        //event.registerEntityRenderer(ModEntities.SCULK_VEX.get(), SculkVexRenderer::new);

    }

    @SubscribeEvent
    public static void registerRenderers(final RegisterParticleProvidersEvent event)
    {
        event.registerSpriteSet(ModParticles.SCULK_CRUST_PARTICLE.get(), SculkCrustParticle.Provider::new);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = SculkHorde.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            MenuScreens.register(ModMenuTypes.SOUL_HARVESTER_MENU.get(), SoulHarvesterScreen::new);
        }
    }
}
