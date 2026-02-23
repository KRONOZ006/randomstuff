package net.kronoz.randomstuff.client;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.entity.client.renderer.OmegaRenderer;
import net.kronoz.randomstuff.entity.client.renderer.SawBladeRenderer;
import net.kronoz.randomstuff.particle.GoldenBurstParticle;
import net.kronoz.randomstuff.particle.HeartBurstParticle;
import net.kronoz.randomstuff.particle.ModParticles;
import net.minecraft.util.Identifier;

public class RandomstuffClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {


        EntityRendererRegistry.register(ModEntities.OMEGA, OmegaRenderer::new);
        EntityRendererRegistry.register(ModEntities.SAWBLADE, SawBladeRenderer::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.GOLDEN_BURST_PARTICLE, GoldenBurstParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.HEART_BURST_PARTICLE, HeartBurstParticle.Factory::new);
    }


}

