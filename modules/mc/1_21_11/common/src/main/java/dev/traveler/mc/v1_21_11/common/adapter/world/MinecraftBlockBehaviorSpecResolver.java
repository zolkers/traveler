package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;

@FunctionalInterface
public interface MinecraftBlockBehaviorSpecResolver {
    Optional<BlockBehaviorSpec> resolve(MinecraftBlockContext context, BlockShape shape);
}
