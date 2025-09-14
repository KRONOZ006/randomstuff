package net.kronoz.randomstuff.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.entity.client.renderer.OmegaRenderer;

public class RandomstuffClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.OMEGA, OmegaRenderer::new);
    }
}
