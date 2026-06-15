package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.TravelerNavigationState;
import java.util.Objects;

@Command("traveler")
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

    @SubRoute("navigate block <x:Integer> <y:Integer> <z:Integer>")
    @Description("Starts Traveler client navigation toward a block")
    CommandResult navigateBlock(@RouteCtx CommandContext context) {
        return jobService.queueNavigateBlock(context, TravelerCommandTargets.blockPosition(context));
    }

    @SubRoute("navigate stop")
    @Description("Stops active Traveler client navigation")
    CommandResult navigateStop(@RouteCtx CommandContext context) {
        String message = "navigation stopped";
        navigationState.stop(message);
        debugState.clearNavigation();
        return TravelerCommandReplies.success(context, message + " | debug nav cleared");
    }
}
