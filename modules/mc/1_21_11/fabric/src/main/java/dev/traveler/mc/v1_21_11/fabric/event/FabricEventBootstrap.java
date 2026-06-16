package dev.traveler.mc.v1_21_11.fabric.event;

import dev.traveler.command.buildmycommand.TravelerCommandModule;
import dev.traveler.core.event.ClientTickEvent;
import dev.traveler.core.event.TravelerClientEvents;
import dev.traveler.core.event.WorldRenderEvent;
import dev.traveler.core.navigation.NavigationRuntime;
import dev.traveler.core.navigation.diagnostics.BlockScanSource;
import dev.traveler.core.navigation.diagnostics.FileMovementFailureReportSink;
import dev.traveler.core.navigation.diagnostics.MovementFailureReportService;
import dev.traveler.core.navigation.diagnostics.MovementFailureReporter;
import dev.traveler.core.render.PathDebugRenderModel;
import dev.traveler.mc.v1_21_11.common.adapter.diagnostics.MinecraftBlockScanSource;
import dev.traveler.mc.v1_21_11.fabric.navigation.MinecraftClientNavigationAdapter;
import dev.traveler.mc.v1_21_11.fabric.render.FabricPathDebugRenderer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.world.phys.Vec3;

public final class FabricEventBootstrap {
    private final Consumer<WorldRenderContext> renderer;
    private final Runnable navigationUpdate;
    private long tickIndex;

    FabricEventBootstrap(Consumer<WorldRenderContext> renderer) {
        this(renderer, () -> {});
    }

    FabricEventBootstrap(Consumer<WorldRenderContext> renderer, Runnable navigationUpdate) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.navigationUpdate = Objects.requireNonNull(navigationUpdate, "navigationUpdate");
    }

    public static void registerClientEvents(TravelerCommandModule module) {
        TravelerCommandModule commandModule = Objects.requireNonNull(module, "module");
        FabricPathDebugRenderer renderer =
                new FabricPathDebugRenderer(
                        PathDebugRenderModel.defaultModel(),
                        commandModule.debugState(),
                        commandModule.navigationState());
        NavigationRuntime navigationRuntime = new NavigationRuntime(
                commandModule.navigationState(),
                MinecraftClientNavigationAdapter.currentClient(),
                commandModule.debugState(),
                movementFailureReporter());
        new FabricEventBootstrap(renderer::render, () -> updateClientWork(commandModule, navigationRuntime)).register();
    }

    void emitClientTick() {
        TravelerClientEvents.CLIENT_TICK.dispatch(new ClientTickEvent(tickIndex));
        tickIndex++;
    }

    void renderWorld(WorldRenderContext context, WorldRenderEvent event) {
        TravelerClientEvents.WORLD_RENDER.dispatch(Objects.requireNonNull(event, "event"));
        navigationUpdate.run();
        renderer.accept(context);
    }

    private void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> emitClientTick());
        WorldRenderEvents.END_MAIN.register(this::renderWorld);
    }

    private static void updateClientWork(TravelerCommandModule commandModule, NavigationRuntime navigationRuntime) {
        commandModule.drainPathJobs();
        navigationRuntime.update(System.nanoTime());
    }

    private static MovementFailureReporter movementFailureReporter() {
        return new MovementFailureReportService(
                FabricEventBootstrap::blockScanSource,
                new FileMovementFailureReportSink(reportDirectory()));
    }

    private static Path reportDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("traveler").resolve("reports");
    }

    private static BlockScanSource blockScanSource() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        return new MinecraftBlockScanSource(client.level);
    }

    private void renderWorld(WorldRenderContext context) {
        renderWorld(context, eventFor(context));
    }

    private static WorldRenderEvent eventFor(WorldRenderContext context) {
        Vec3 camera = FabricPathDebugRenderer.cameraPosition(context).orElse(Vec3.ZERO);
        return new WorldRenderEvent(0.0f, camera.x, camera.y, camera.z);
    }
}
