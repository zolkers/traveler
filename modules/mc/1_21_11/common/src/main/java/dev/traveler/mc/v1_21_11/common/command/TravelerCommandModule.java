package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.core.CommandFramework;
import dev.traveler.core.command.TravelerCommandCatalog;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.mc.v1_21_11.common.adapter.MinecraftWorldSnapshot;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.BlockGetter;

public final class TravelerCommandModule {
    private final TravelerCommandCatalog catalog;
    private final CommandFramework framework;
    private final PathfinderDebugState debugState;

    public TravelerCommandModule() {
        this(new PathfinderDebugState(), () -> null);
    }

    TravelerCommandModule(WorldLayer worldLayer) {
        this(new PathfinderDebugState(), Objects.requireNonNull(worldLayer, "worldLayer"));
    }

    public TravelerCommandModule(PathfinderDebugState debugState, Supplier<? extends BlockGetter> blockGetterSupplier) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        Supplier<? extends BlockGetter> blockGetterProvider =
                Objects.requireNonNull(blockGetterSupplier, "blockGetterSupplier");
        PathTravelerCommandFeature pathFeature = new PathTravelerCommandFeature(
                debugState, () -> worldLayerFor(blockGetterProvider.get()));
        catalog = TravelerCommandCatalog.fromFeatures(pathFeature);
        framework = CommandFramework.builder().build();
        new BuildMyCommandCatalogAdapter(framework.registry()).register(catalog);
    }

    private TravelerCommandModule(PathfinderDebugState debugState, WorldLayer worldLayer) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        PathTravelerCommandFeature pathFeature =
                new PathTravelerCommandFeature(debugState, () -> worldLayer);
        catalog = TravelerCommandCatalog.fromFeatures(pathFeature);
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

    private static WorldLayer worldLayerFor(BlockGetter blockGetter) {
        if (blockGetter == null) {
            return null;
        }
        return new MinecraftWorldSnapshot(blockGetter);
    }
}
