package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommand;
import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.command.TravelerCommandResult;
import dev.traveler.core.command.TravelerSubcommand;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.navigation.follow.NavigationPath;
import java.util.Objects;
import java.util.Optional;

@TravelerCommand(root = "traveler navigate")
public final class NavigateTravelerCommandFeature {
    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;
    private final TravelerPathSearchService searchService;

    NavigateTravelerCommandFeature(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            TravelerPathSearchService searchService) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        this.searchService = Objects.requireNonNull(searchService, "searchService");
    }

    @TravelerSubcommand(
            route = "block <x:int> <y:int> <z:int>",
            description = "Starts Traveler client navigation toward a block")
    private TravelerCommandResult navigateBlock(TravelerCommandContext context) {
        TravelerPathSearchResult result =
                searchService.blockPath(context, TravelerCommandTargets.blockPosition(context));
        result.updateDebug(debugState);
        Optional<NavigationPath> path = result.navigationPath();
        if (path.isEmpty()) {
            return navigationFailure(result);
        }
        String message = result.message().replaceFirst("^path", "navigate");
        navigationState.start(path.orElseThrow(), message);
        return TravelerCommandResult.success(message);
    }

    @TravelerSubcommand(route = "stop", description = "Stops active Traveler client navigation")
    private TravelerCommandResult navigateStop(TravelerCommandContext context) {
        String message = "navigation stopped";
        navigationState.stop(message);
        return TravelerCommandResult.success(message);
    }

    private TravelerCommandResult navigationFailure(TravelerPathSearchResult result) {
        String message = "navigation not started status=" + result.status();
        navigationState.stop(message);
        return TravelerCommandResult.failure(message);
    }
}
