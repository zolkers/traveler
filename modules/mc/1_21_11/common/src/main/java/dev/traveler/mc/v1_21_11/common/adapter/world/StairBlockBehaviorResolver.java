package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.StairBlock;

final class StairBlockBehaviorResolver implements MinecraftBlockBehaviorResolver {
    @Override
    public Optional<BlockBehavior> resolve(
            MinecraftBlockContext context,
            BlockShape shape,
            BlockBehaviorRegistry behaviorRegistry) {
        if (!StairBlock.isStairs(context.state())) {
            return Optional.empty();
        }
        Direction direction = context.state().getValue(StairBlock.FACING);
        return Optional.of(behaviorRegistry.stair(horizontalFacing(direction)));
    }

    private static HorizontalFacing horizontalFacing(Direction direction) {
        return switch (Objects.requireNonNull(direction, "direction")) {
            case NORTH -> HorizontalFacing.NORTH;
            case SOUTH -> HorizontalFacing.SOUTH;
            case WEST -> HorizontalFacing.WEST;
            case EAST -> HorizontalFacing.EAST;
            default -> throw new IllegalArgumentException("Expected horizontal stair facing, got " + direction + ".");
        };
    }
}
