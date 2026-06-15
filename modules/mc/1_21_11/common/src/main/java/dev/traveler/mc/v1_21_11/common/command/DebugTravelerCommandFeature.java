package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.core.debug.DebugTextFormatter;
import dev.traveler.core.debug.PathfinderDebugState;
import java.time.Instant;
import java.util.Objects;

@Command("traveler")
public final class DebugTravelerCommandFeature {
    private final PathfinderDebugState debugState;

    DebugTravelerCommandFeature(PathfinderDebugState debugState) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
    }

    @SubRoute("debug status")
    @Description("Prints Traveler path and navigation debug state")
    CommandResult status(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.success(
                context,
                DebugTextFormatter.detailedStatus(
                        debugState.latestNavigation(),
                        debugState.latestSnapshot(),
                        Instant.now()));
    }

    @SubRoute("debug clear")
    @Description("Clears Traveler path and navigation debug state")
    CommandResult clear(@RouteCtx CommandContext context) {
        debugState.clear();
        return TravelerCommandReplies.success(context, "debug cleared");
    }
}
