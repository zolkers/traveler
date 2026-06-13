package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommand;
import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.command.TravelerCommandResult;
import dev.traveler.core.command.TravelerSubcommand;
import dev.traveler.core.debug.DebugTextFormatter;
import dev.traveler.core.debug.PathfinderDebugState;
import java.util.Objects;

@TravelerCommand(root = "traveler debug")
public final class DebugTravelerCommandFeature {
    private static final String NO_NAVIGATION = "nav=none";
    private static final String NO_PATH = "path=none";

    private final PathfinderDebugState debugState;

    DebugTravelerCommandFeature(PathfinderDebugState debugState) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
    }

    @TravelerSubcommand(route = "status", description = "Prints Traveler path and navigation debug state")
    private TravelerCommandResult status(TravelerCommandContext context) {
        return TravelerCommandResult.success("debug " + navigationSummary() + " | " + pathSummary());
    }

    @TravelerSubcommand(route = "clear", description = "Clears Traveler path and navigation debug state")
    private TravelerCommandResult clear(TravelerCommandContext context) {
        debugState.clear();
        return TravelerCommandResult.success("debug cleared");
    }

    private String navigationSummary() {
        return debugState.navigationSummary().orElse(NO_NAVIGATION);
    }

    private String pathSummary() {
        return debugState.latestSnapshot()
                .map(DebugTextFormatter::pathSummary)
                .orElse(NO_PATH);
    }
}
