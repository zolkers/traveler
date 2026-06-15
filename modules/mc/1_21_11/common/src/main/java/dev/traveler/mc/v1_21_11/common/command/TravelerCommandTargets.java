package dev.traveler.mc.v1_21_11.common.command;

import dev.riege.buildmycommand.api.CommandContext;
import dev.traveler.core.world.block.BlockPosition;

final class TravelerCommandTargets {
    private TravelerCommandTargets() {}

    static BlockPosition blockPosition(CommandContext context) {
        return new BlockPosition(
                context.arg("x", Integer.class),
                context.arg("y", Integer.class),
                context.arg("z", Integer.class));
    }
}
