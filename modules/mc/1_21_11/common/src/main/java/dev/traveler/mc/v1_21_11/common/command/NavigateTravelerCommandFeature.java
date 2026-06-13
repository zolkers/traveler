package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommand;
import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.command.TravelerCommandResult;
import dev.traveler.core.command.TravelerSubcommand;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.TravelerNavigationState;
import java.util.Objects;

@TravelerCommand(root = "traveler navigate")
public final class NavigateTravelerCommandFeature {
    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;
    private final TravelerPathJobService jobService;

    NavigateTravelerCommandFeature(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            TravelerPathJobService jobService) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        this.jobService = Objects.requireNonNull(jobService, "jobService");
    }

    @TravelerSubcommand(
            route = "block <x:int> <y:int> <z:int>",
            description = "Starts Traveler client navigation toward a block")
    private TravelerCommandResult navigateBlock(TravelerCommandContext context) {
        return jobService.queueNavigateBlock(context, TravelerCommandTargets.blockPosition(context));
    }

    @TravelerSubcommand(route = "stop", description = "Stops active Traveler client navigation")
    private TravelerCommandResult navigateStop(TravelerCommandContext context) {
        String message = "navigation stopped";
        navigationState.stop(message);
        debugState.clearNavigation();
        return TravelerCommandResult.success(message + " | debug nav cleared");
    }
}
