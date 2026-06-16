package dev.traveler.core.capability.behavior.noop;

import dev.traveler.core.capability.behavior.api.BlockBehaviorModule;
import dev.traveler.core.capability.behavior.spi.BlockBehaviorResolver;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;

public final class UnknownBlockBehaviorModule implements BlockBehaviorModule {
    private final PathfinderModuleDescriptor descriptor =
            new PathfinderModuleDescriptor(
                    "behavior.unknown",
                    PathfinderModuleType.BEHAVIOR,
                    Integer.MAX_VALUE,
                    false);

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<BlockBehaviorResolver> resolvers() {
        return List.of();
    }
}
