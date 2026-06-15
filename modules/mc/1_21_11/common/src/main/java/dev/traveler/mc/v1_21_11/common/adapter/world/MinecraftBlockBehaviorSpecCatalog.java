package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;

final class MinecraftBlockBehaviorSpecCatalog {
    private MinecraftBlockBehaviorSpecCatalog() {}

    static BlockBehaviorSpec resolve(MinecraftBlockContext context, BlockShape shape) {
        return CarpetBlockSpecResolver.resolve(context, shape)
                .or(() -> LadderBlockSpecResolver.resolve(context, shape))
                .or(() -> VineBlockSpecResolver.resolve(context, shape))
                .or(() -> FenceBlockSpecResolver.resolve(context, shape))
                .or(() -> WallBlockSpecResolver.resolve(context, shape))
                .or(() -> SlabBlockSpecResolver.resolve(context, shape))
                .or(() -> StairBlockSpecResolver.resolve(context, shape))
                .orElseGet(BlockBehaviorSpec::automatic);
    }
}
