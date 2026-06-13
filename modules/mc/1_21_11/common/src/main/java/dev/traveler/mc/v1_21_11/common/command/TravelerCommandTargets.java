package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.world.block.BlockPosition;

final class TravelerCommandTargets {
    private TravelerCommandTargets() {}

    static BlockPosition blockPosition(TravelerCommandContext context) {
        return new BlockPosition(
                context.arg("x", int.class),
                context.arg("y", int.class),
                context.arg("z", int.class));
    }
}
