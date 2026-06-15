package dev.traveler.command.buildmycommand;

import dev.riege.buildmycommand.api.CommandContext;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;

final class TravelerCommandTargets {
    private TravelerCommandTargets() {
    }

    static BlockPosition blockPosition(CommandContext context) {
        return new BlockPosition(
                context.arg("x", Integer.class),
                context.arg("y", Integer.class),
                context.arg("z", Integer.class));
    }

    static RouteGoal xyzGoal(CommandContext context) {
        return RouteGoal.xyz(
                context.arg("x", Integer.class),
                context.arg("y", Integer.class),
                context.arg("z", Integer.class));
    }

    static RouteGoal xzGoal(CommandContext context) {
        return RouteGoal.xz(
                context.arg("x", Integer.class),
                context.arg("z", Integer.class));
    }

    static RouteGoal yGoal(CommandContext context) {
        return RouteGoal.yLevel(context.arg("y", Integer.class));
    }
}
