package dev.traveler.mc.v1_21_11.fabric.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

@FunctionalInterface
public interface FabricWorldRenderer {
    void render(WorldRenderContext context);
}
