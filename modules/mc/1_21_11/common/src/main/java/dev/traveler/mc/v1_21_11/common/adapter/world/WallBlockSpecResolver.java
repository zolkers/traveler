package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;
import net.minecraft.world.level.block.WallBlock;

final class WallBlockSpecResolver {
    private WallBlockSpecResolver() {}

    static Optional<BlockBehaviorSpec> resolve(MinecraftBlockContext context, BlockShape shape) {
        if (!(context.state().getBlock() instanceof WallBlock)) {
            return Optional.empty();
        }
        return Optional.of(BlockBehaviorSpec.wall());
    }
}
