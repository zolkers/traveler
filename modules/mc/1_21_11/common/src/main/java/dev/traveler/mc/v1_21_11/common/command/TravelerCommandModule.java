package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.traveler.core.command.TravelerCommandRuntime;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.BlockGetter;

public final class TravelerCommandModule {
    private final CommandFramework framework;
    private final TravelerCommandRuntime runtime;

    public TravelerCommandModule() {
        this(new PathfinderDebugState(), new TravelerNavigationState(), (WorldLayerSupplier) () -> null);
    }

    TravelerCommandModule(WorldLayer worldLayer) {
        this(
                new PathfinderDebugState(),
                new TravelerNavigationState(),
                Objects.requireNonNull(worldLayer, "worldLayer"));
    }

    public TravelerCommandModule(PathfinderDebugState debugState, Supplier<? extends BlockGetter> blockGetterSupplier) {
        this(debugState, new TravelerNavigationState(), worldLayerSupplier(blockGetterSupplier));
    }

    public TravelerCommandModule(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            Supplier<? extends BlockGetter> blockGetterSupplier) {
        this(debugState, navigationState, worldLayerSupplier(blockGetterSupplier));
    }

    private TravelerCommandModule(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            WorldLayer worldLayer) {
        this(debugState, navigationState, () -> worldLayer);
    }

    private TravelerCommandModule(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            WorldLayerSupplier worldLayerSupplier) {
        runtime = new TravelerCommandRuntime(
                Objects.requireNonNull(debugState, "debugState"),
                Objects.requireNonNull(navigationState, "navigationState"),
                worldLayerSupplier);
        PathTravelerCommandFeature pathFeature = new PathTravelerCommandFeature(runtime.pathCommands());
        NavigateTravelerCommandFeature navigateFeature = new NavigateTravelerCommandFeature(runtime.navigateCommands());
        DebugTravelerCommandFeature debugFeature = new DebugTravelerCommandFeature(runtime.debugCommands());
        framework = CommandFramework.builder().build();
        AnnotationCommandScanner.register(framework.registry(), pathFeature);
        AnnotationCommandScanner.register(framework.registry(), navigateFeature);
        AnnotationCommandScanner.register(framework.registry(), debugFeature);
    }

    public CommandFramework framework() {
        return framework;
    }

    public PathfinderDebugState debugState() {
        return runtime.debugState();
    }

    public TravelerNavigationState navigationState() {
        return runtime.navigationState();
    }

    public void drainPathJobs() {
        runtime.drainPathJobs();
    }

    private static WorldLayerSupplier worldLayerSupplier(Supplier<? extends BlockGetter> blockGetterSupplier) {
        Supplier<? extends BlockGetter> blockGetterProvider =
                Objects.requireNonNull(blockGetterSupplier, "blockGetterSupplier");
        return () -> worldLayerFor(blockGetterProvider.get());
    }

    private static WorldLayer worldLayerFor(BlockGetter blockGetter) {
        if (blockGetter == null) {
            return null;
        }
        return new MinecraftWorldSnapshot(blockGetter);
    }

    @FunctionalInterface
    private interface WorldLayerSupplier extends Supplier<WorldLayer> {}
}
