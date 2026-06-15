package dev.traveler.command.buildmycommand;

import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.core.command.TravelerPathCommandHandler;
import java.util.Objects;

@Command("traveler")
final class PathTravelerCommandFeature {
    private final TravelerPathCommandHandler handler;

    PathTravelerCommandFeature(TravelerPathCommandHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    @SubRoute("path test")
    @Description("Runs a Traveler path debug search")
    CommandResult pathTest(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.result(handler.pathTest(BuildMyCommandTravelerSource.from(context)));
    }

    @SubRoute("path block <x:Integer> <y:Integer> <z:Integer>")
    @Description("Runs a Traveler path debug search for a block")
    CommandResult pathBlock(@RouteCtx CommandContext context) {
        BuildMyCommandTravelerSource source = BuildMyCommandTravelerSource.from(context);
        return TravelerCommandReplies.result(
                handler.pathBlock(source, TravelerCommandTargets.blockPosition(context)));
    }
}
