package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.command.buildmycommand.TravelerCommandModule;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.BlockGetter;

public final class MinecraftTravelerCommandBridge {
    private MinecraftTravelerCommandBridge() {
    }

    public static TravelerCommandModule create(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            Supplier<? extends BlockGetter> blockGetterSupplier) {
        return new TravelerCommandModule(debugState, navigationState, worldLayerSupplier(blockGetterSupplier));
    }

    public static TravelerCommandModule create(
            PathfinderDebugState debugState,
            Supplier<? extends BlockGetter> blockGetterSupplier) {
        return new TravelerCommandModule(debugState, worldLayerSupplier(blockGetterSupplier));
    }

    private static Supplier<WorldLayer> worldLayerSupplier(Supplier<? extends BlockGetter> blockGetterSupplier) {
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
}
