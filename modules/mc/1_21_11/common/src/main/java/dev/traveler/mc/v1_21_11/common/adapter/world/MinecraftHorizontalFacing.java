package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.world.behavior.context.HorizontalFacing;
import java.util.Objects;
import net.minecraft.core.Direction;

final class MinecraftHorizontalFacing {
    private MinecraftHorizontalFacing() {}

    static HorizontalFacing from(Direction direction) {
        return switch (Objects.requireNonNull(direction, "direction")) {
            case NORTH -> HorizontalFacing.NORTH;
            case SOUTH -> HorizontalFacing.SOUTH;
            case WEST -> HorizontalFacing.WEST;
            case EAST -> HorizontalFacing.EAST;
            default -> throw new IllegalArgumentException("Expected horizontal direction, got " + direction + ".");
        };
    }
}
