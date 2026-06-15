package dev.traveler.core.command;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import java.util.Objects;
import java.util.function.Supplier;

public final class TravelerCommandRuntime implements AutoCloseable {
    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;
    private final TravelerPathJobService pathJobService;
    private final TravelerPathCommandHandler pathCommands;
    private final TravelerNavigateCommandHandler navigateCommands;
    private final TravelerDebugCommandHandler debugCommands;

    public TravelerCommandRuntime(Supplier<? extends WorldLayer> worldLayerSupplier) {
        this(new PathfinderDebugState(), new TravelerNavigationState(), worldLayerSupplier);
    }

    public TravelerCommandRuntime(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            Supplier<? extends WorldLayer> worldLayerSupplier) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        TravelerPathSearchService searchService = new TravelerPathSearchService(worldLayerSupplier);
        pathJobService = new TravelerPathJobService(this.debugState, this.navigationState, searchService);
        pathCommands = new TravelerPathCommandHandler(this.debugState, searchService, pathJobService);
        navigateCommands = new TravelerNavigateCommandHandler(this.debugState, this.navigationState, pathJobService);
        debugCommands = new TravelerDebugCommandHandler(this.debugState);
    }

    public PathfinderDebugState debugState() {
        return debugState;
    }

    public TravelerNavigationState navigationState() {
        return navigationState;
    }

    public TravelerPathCommandHandler pathCommands() {
        return pathCommands;
    }

    public TravelerNavigateCommandHandler navigateCommands() {
        return navigateCommands;
    }

    public TravelerDebugCommandHandler debugCommands() {
        return debugCommands;
    }

    public void drainPathJobs() {
        pathJobService.drainCompleted();
    }

    @Override
    public void close() {
        pathJobService.close();
    }
}
