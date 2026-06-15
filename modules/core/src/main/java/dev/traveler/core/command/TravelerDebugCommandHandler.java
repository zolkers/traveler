package dev.traveler.core.command;

import dev.traveler.core.debug.DebugTextFormatter;
import dev.traveler.core.debug.PathfinderDebugState;
import java.time.Instant;
import java.util.Objects;

public final class TravelerDebugCommandHandler {
    private final PathfinderDebugState debugState;

    TravelerDebugCommandHandler(PathfinderDebugState debugState) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
    }

    public TravelerCommandResponse status(TravelerCommandSource source) {
        return status(source, Instant.now());
    }

    TravelerCommandResponse status(TravelerCommandSource source, Instant now) {
        return TravelerCommandResponse.reply(
                source,
                DebugTextFormatter.detailedStatus(
                        debugState.latestNavigation(),
                        debugState.latestSnapshot(),
                        now));
    }

    public TravelerCommandResponse clear(TravelerCommandSource source) {
        debugState.clear();
        return TravelerCommandResponse.reply(source, "debug cleared");
    }
}
