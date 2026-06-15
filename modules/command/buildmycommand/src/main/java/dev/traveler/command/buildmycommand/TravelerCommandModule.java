package dev.traveler.command.buildmycommand;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.traveler.core.command.TravelerCommandRuntime;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import java.util.Objects;
import java.util.function.Supplier;

public final class TravelerCommandModule {
    private final CommandFramework framework;
    private final TravelerCommandRuntime runtime;

    public TravelerCommandModule() {
        this(new TravelerCommandRuntime(new PathfinderDebugState(), new TravelerNavigationState(), () -> null));
    }

    public TravelerCommandModule(WorldLayer worldLayer) {
        this(new TravelerCommandRuntime(
                new PathfinderDebugState(),
                new TravelerNavigationState(),
                () -> Objects.requireNonNull(worldLayer, "worldLayer")));
    }

    public TravelerCommandModule(PathfinderDebugState debugState, Supplier<? extends WorldLayer> worldLayerSupplier) {
        this(new TravelerCommandRuntime(
                debugState,
                new TravelerNavigationState(),
                Objects.requireNonNull(worldLayerSupplier, "worldLayerSupplier")));
    }

    public TravelerCommandModule(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            Supplier<? extends WorldLayer> worldLayerSupplier) {
        this(new TravelerCommandRuntime(
                debugState,
                navigationState,
                Objects.requireNonNull(worldLayerSupplier, "worldLayerSupplier")));
    }

    private TravelerCommandModule(TravelerCommandRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
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
}
