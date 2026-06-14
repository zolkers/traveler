package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;

final class AirBlockBehaviorResolver implements MinecraftBlockBehaviorResolver {
    @Override
    public Optional<BlockBehavior> resolve(
            MinecraftBlockContext context,
            BlockShape shape,
            BlockBehaviorRegistry behaviorRegistry) {
        if (!shape.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(behaviorRegistry.behavior(BlockBehaviorKey.AIR));
    }
}
