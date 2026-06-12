package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.core.CommandFramework;
import dev.traveler.core.command.TravelerCommandCatalog;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.BlockGetter;

public final class TravelerCommandModule {
    private final TravelerCommandCatalog catalog;
    private final CommandFramework framework;
    private final PathfinderDebugState debugState;

    public TravelerCommandModule() {
        this(new PathfinderDebugState(), (WorldLayerSupplier) () -> null);
    }

    TravelerCommandModule(WorldLayer worldLayer) {
        this(new PathfinderDebugState(), Objects.requireNonNull(worldLayer, "worldLayer"));
    }

    public TravelerCommandModule(PathfinderDebugState debugState, Supplier<? extends BlockGetter> blockGetterSupplier) {
        this(debugState, worldLayerSupplier(blockGetterSupplier));
    }

    private TravelerCommandModule(PathfinderDebugState debugState, WorldLayer worldLayer) {
        this(debugState, () -> worldLayer);
    }

    private TravelerCommandModule(PathfinderDebugState debugState, WorldLayerSupplier worldLayerSupplier) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        PathTravelerCommandFeature pathFeature = new PathTravelerCommandFeature(this.debugState, worldLayerSupplier);
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
