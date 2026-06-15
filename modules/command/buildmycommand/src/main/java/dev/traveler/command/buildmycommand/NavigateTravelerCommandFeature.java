package dev.traveler.command.buildmycommand;

import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.core.command.TravelerNavigateCommandHandler;
import java.util.Objects;

@Command("traveler")
final class NavigateTravelerCommandFeature {
    private final TravelerNavigateCommandHandler handler;

    NavigateTravelerCommandFeature(TravelerNavigateCommandHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    @SubRoute("navigate block <x:Integer> <y:Integer> <z:Integer>")
    @Description("Starts Traveler client navigation toward a block")
    CommandResult navigateBlock(@RouteCtx CommandContext context) {
        BuildMyCommandTravelerSource source = BuildMyCommandTravelerSource.from(context);
        return TravelerCommandReplies.result(
                handler.block(source, TravelerCommandTargets.blockPosition(context)));
    }

    @SubRoute("navigate stop")
    @Description("Stops active Traveler client navigation")
    CommandResult navigateStop(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.result(handler.stop(BuildMyCommandTravelerSource.from(context)));
    }
}
