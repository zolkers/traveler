package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LadderBlock;

final class LadderBlockSpecResolver {
    private LadderBlockSpecResolver() {}

    static Optional<BlockBehaviorSpec> resolve(MinecraftBlockContext context, BlockShape shape) {
        if (!(context.state().getBlock() instanceof LadderBlock)) {
            return Optional.empty();
        }
        Direction direction = context.state().getValue(LadderBlock.FACING);
        return Optional.of(BlockBehaviorSpec.ladder(MinecraftHorizontalFacing.from(direction)));
    }
}
