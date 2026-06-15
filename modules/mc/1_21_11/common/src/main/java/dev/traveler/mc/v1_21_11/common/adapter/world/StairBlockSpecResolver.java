package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.StairBlock;

final class StairBlockSpecResolver {
    private StairBlockSpecResolver() {}

    static Optional<BlockBehaviorSpec> resolve(MinecraftBlockContext context, BlockShape shape) {
        if (!StairBlock.isStairs(context.state())) {
            return Optional.empty();
        }
        Direction direction = context.state().getValue(StairBlock.FACING);
        return Optional.of(BlockBehaviorSpec.stair(MinecraftHorizontalFacing.from(direction)));
    }
}
