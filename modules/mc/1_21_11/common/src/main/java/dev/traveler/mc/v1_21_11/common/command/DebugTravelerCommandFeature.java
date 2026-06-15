package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.core.command.TravelerDebugCommandHandler;
import java.util.Objects;

@Command("traveler")
public final class DebugTravelerCommandFeature {
    private final TravelerDebugCommandHandler handler;

    DebugTravelerCommandFeature(TravelerDebugCommandHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    @SubRoute("debug status")
    @Description("Prints Traveler path and navigation debug state")
    CommandResult status(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.result(handler.status(BuildMyCommandTravelerSource.from(context)));
    }

    @SubRoute("debug clear")
    @Description("Clears Traveler path and navigation debug state")
    CommandResult clear(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.result(handler.clear(BuildMyCommandTravelerSource.from(context)));
    }
}
