package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.BlockGetter;

public final class TravelerCommandModule {
    private final CommandFramework framework;
    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;
    private final TravelerPathJobService pathJobService;

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
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        TravelerPathSearchService searchService = new TravelerPathSearchService(worldLayerSupplier);
        pathJobService = new TravelerPathJobService(this.debugState, this.navigationState, searchService);
        PathTravelerCommandFeature pathFeature =
                new PathTravelerCommandFeature(this.debugState, searchService, pathJobService);
        NavigateTravelerCommandFeature navigateFeature =
                new NavigateTravelerCommandFeature(this.debugState, this.navigationState, pathJobService);
        DebugTravelerCommandFeature debugFeature = new DebugTravelerCommandFeature(this.debugState);
        framework = CommandFramework.builder().build();
        AnnotationCommandScanner.register(framework.registry(), pathFeature);
        AnnotationCommandScanner.register(framework.registry(), navigateFeature);
        AnnotationCommandScanner.register(framework.registry(), debugFeature);
    }

    public CommandFramework framework() {
        return framework;
    }

    public PathfinderDebugState debugState() {
        return debugState;
    }

    public TravelerNavigationState navigationState() {
        return navigationState;
    }

    public void drainPathJobs() {
        pathJobService.drainCompleted();
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
