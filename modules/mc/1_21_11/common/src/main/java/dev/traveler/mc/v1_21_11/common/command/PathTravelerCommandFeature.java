package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import java.util.Objects;
import java.util.function.Supplier;

@Command("traveler")
public final class PathTravelerCommandFeature {
    private final PathfinderDebugState debugState;
    private final TravelerPathSearchService searchService;
    private final TravelerPathJobService jobService;

    public PathTravelerCommandFeature(
            PathfinderDebugState debugState,
            Supplier<? extends WorldLayer> worldLayerSupplier) {
        this(debugState, new TravelerPathSearchService(worldLayerSupplier));
    }

    private PathTravelerCommandFeature(PathfinderDebugState debugState, TravelerPathSearchService searchService) {
        this(
                debugState,
                searchService,
                new TravelerPathJobService(debugState, new TravelerNavigationState(), searchService));
    }

    PathTravelerCommandFeature(
            PathfinderDebugState debugState,
            TravelerPathSearchService searchService,
            TravelerPathJobService jobService) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.searchService = Objects.requireNonNull(searchService, "searchService");
        this.jobService = Objects.requireNonNull(jobService, "jobService");
    }

    @SubRoute("path test")
    @Description("Runs a Traveler path debug search")
    CommandResult pathTest(@RouteCtx CommandContext context) {
        TravelerPathSearchResult result = searchService.testPath();
        result.updateDebug(debugState);
        return TravelerCommandReplies.success(context, result.message());
    }

    @SubRoute("path block <x:Integer> <y:Integer> <z:Integer>")
    @Description("Runs a Traveler path debug search for a block")
    CommandResult pathBlock(@RouteCtx CommandContext context) {
        return jobService.queuePathBlock(context, TravelerCommandTargets.blockPosition(context));
    }
}
