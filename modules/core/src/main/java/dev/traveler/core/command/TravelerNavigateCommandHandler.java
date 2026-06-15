package dev.traveler.core.command;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public final class TravelerNavigateCommandHandler {
    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;
    private final TravelerPathJobService jobService;

    TravelerNavigateCommandHandler(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            TravelerPathJobService jobService) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        this.jobService = Objects.requireNonNull(jobService, "jobService");
    }

    public TravelerCommandResponse block(TravelerCommandSource source, BlockPosition target) {
        return jobService.queueNavigateBlock(source, target);
    }

    public TravelerCommandResponse stop(TravelerCommandSource source) {
        String message = "navigation stopped";
        navigationState.stop(message);
        debugState.clearNavigation();
        return TravelerCommandResponse.reply(source, message + " | debug nav cleared");
    }
}
