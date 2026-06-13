package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommand;
import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.command.TravelerCommandResult;
import dev.traveler.core.command.TravelerSubcommand;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import java.util.Objects;
import java.util.function.Supplier;

@TravelerCommand(root = "traveler path")
public final class PathTravelerCommandFeature {
    private final PathfinderDebugState debugState;
    private final TravelerPathSearchService searchService;

    public PathTravelerCommandFeature(
            PathfinderDebugState debugState,
            Supplier<? extends WorldLayer> worldLayerSupplier) {
        this(debugState, new TravelerPathSearchService(worldLayerSupplier));
    }

    PathTravelerCommandFeature(PathfinderDebugState debugState, TravelerPathSearchService searchService) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.searchService = Objects.requireNonNull(searchService, "searchService");
    }

    @TravelerSubcommand(route = "test", description = "Runs a Traveler path debug search")
    private TravelerCommandResult pathTest(TravelerCommandContext context) {
        TravelerPathSearchResult result = searchService.testPath();
        result.updateDebug(debugState);
        return TravelerCommandResult.success(result.message());
    }

    @TravelerSubcommand(
            route = "block <x:int> <y:int> <z:int>",
            description = "Runs a Traveler path debug search for a block")
    private TravelerCommandResult pathBlock(TravelerCommandContext context) {
        TravelerPathSearchResult result =
                searchService.blockPath(context, TravelerCommandTargets.blockPosition(context));
        result.updateDebug(debugState);
        return TravelerCommandResult.success(result.message());
    }
}
