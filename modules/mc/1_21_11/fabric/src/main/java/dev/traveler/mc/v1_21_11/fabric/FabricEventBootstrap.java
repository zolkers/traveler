package dev.traveler.mc.v1_21_11.fabric;

import dev.traveler.mc.v1_21_11.common.command.TravelerCommandModule;
import dev.traveler.core.event.ClientTickEvent;
import dev.traveler.core.event.TravelerClientEvents;
import dev.traveler.core.event.WorldRenderEvent;
import dev.traveler.core.render.PathDebugRenderModel;
import java.util.Objects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.world.phys.Vec3;

public final class FabricEventBootstrap {
    private final FabricWorldRenderer renderer;
    private long tickIndex;

    FabricEventBootstrap(FabricWorldRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public static void registerClientEvents(TravelerCommandModule module) {
        TravelerCommandModule commandModule = Objects.requireNonNull(module, "module");
        FabricPathDebugRenderer renderer =
                new FabricPathDebugRenderer(PathDebugRenderModel.defaultModel(), commandModule.debugState());
        new FabricEventBootstrap(renderer).register();
    }

    void emitClientTick() {
        TravelerClientEvents.CLIENT_TICK.dispatch(new ClientTickEvent(tickIndex));
        tickIndex++;
    }

    void renderWorld(WorldRenderContext context, WorldRenderEvent event) {
        TravelerClientEvents.WORLD_RENDER.dispatch(Objects.requireNonNull(event, "event"));
        renderer.render(context);
    }

    private void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> emitClientTick());
        WorldRenderEvents.END_MAIN.register(this::renderWorld);
    }

    private void renderWorld(WorldRenderContext context) {
        renderWorld(context, eventFor(context));
    }

    private static WorldRenderEvent eventFor(WorldRenderContext context) {
        Vec3 camera = FabricPathDebugRenderer.cameraPosition(context).orElse(Vec3.ZERO);
        return new WorldRenderEvent(0.0f, camera.x, camera.y, camera.z);
    }
}
