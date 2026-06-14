package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface MinecraftBlockBehaviorResolver {
    Optional<BlockBehavior> resolve(
            MinecraftBlockContext context,
            BlockShape shape,
            BlockBehaviorRegistry behaviorRegistry);

    static List<MinecraftBlockBehaviorResolver> defaults() {
        return List.of(
                new BarrierBlockBehaviorResolver(),
                new CarpetBlockBehaviorResolver(),
                new LadderBlockBehaviorResolver(),
                new VineBlockBehaviorResolver(),
                new AirBlockBehaviorResolver(),
                new SlabBlockBehaviorResolver(),
                new StairBlockBehaviorResolver(),
                new FullBlockBehaviorResolver());
    }
}
