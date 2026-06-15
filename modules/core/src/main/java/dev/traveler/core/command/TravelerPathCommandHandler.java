package dev.traveler.core.command;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public final class TravelerPathCommandHandler {
    private final PathfinderDebugState debugState;
    private final TravelerPathSearchService searchService;
    private final TravelerPathJobService jobService;

    TravelerPathCommandHandler(
            PathfinderDebugState debugState,
            TravelerPathSearchService searchService,
            TravelerPathJobService jobService) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.searchService = Objects.requireNonNull(searchService, "searchService");
        this.jobService = Objects.requireNonNull(jobService, "jobService");
    }

    public TravelerCommandResponse pathTest(TravelerCommandSource source) {
        TravelerPathSearchResult result = searchService.testPath();
        result.updateDebug(debugState);
        return TravelerCommandResponse.reply(source, result.message());
    }

    public TravelerCommandResponse pathBlock(TravelerCommandSource source, BlockPosition target) {
        return jobService.queuePathBlock(source, target);
    }
}
