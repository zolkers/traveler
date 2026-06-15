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

    @SubRoute("path xyz <x:Integer> <y:Integer> <z:Integer>")
    @Description("Runs a Traveler path debug search for an exact XYZ goal")
    CommandResult pathXyz(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.result(handler.pathGoal(
                BuildMyCommandTravelerSource.from(context),
                TravelerCommandTargets.xyzGoal(context)));
    }

    @SubRoute("path xz <x:Integer> <z:Integer>")
    @Description("Runs a Traveler path debug search for an XZ goal")
    CommandResult pathXz(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.result(handler.pathGoal(
                BuildMyCommandTravelerSource.from(context),
                TravelerCommandTargets.xzGoal(context)));
    }

    @SubRoute("path y <y:Integer>")
    @Description("Runs a Traveler path debug search for a Y-level goal")
    CommandResult pathY(@RouteCtx CommandContext context) {
        return TravelerCommandReplies.result(handler.pathGoal(
                BuildMyCommandTravelerSource.from(context),
                TravelerCommandTargets.yGoal(context)));
    }
}
