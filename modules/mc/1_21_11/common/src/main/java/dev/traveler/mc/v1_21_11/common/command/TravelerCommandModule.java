package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.core.CommandFramework;
import dev.traveler.core.command.AnnotatedTravelerCommandFeature;
import dev.traveler.core.command.TravelerCommandCatalog;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.BlockGetter;

public final class TravelerCommandModule {
    private final TravelerCommandCatalog catalog;
    private final CommandFramework framework;
    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;

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
        PathTravelerCommandFeature pathFeature = new PathTravelerCommandFeature(this.debugState, searchService);
        NavigateTravelerCommandFeature navigateFeature =
                new NavigateTravelerCommandFeature(this.debugState, this.navigationState, searchService);
        DebugTravelerCommandFeature debugFeature = new DebugTravelerCommandFeature(this.debugState);
        catalog = TravelerCommandCatalog.fromFeatures(
                AnnotatedTravelerCommandFeature.from(pathFeature),
                AnnotatedTravelerCommandFeature.from(navigateFeature),
                AnnotatedTravelerCommandFeature.from(debugFeature));
        framework = CommandFramework.builder().build();
        new BuildMyCommandCatalogAdapter(framework.registry()).register(catalog);
    }

    public TravelerCommandCatalog catalog() {
        return catalog;
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
