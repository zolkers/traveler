package dev.traveler.core.capability.behavior.api;

import dev.traveler.core.capability.behavior.spi.BlockBehaviorResolver;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.List;

public interface BlockBehaviorModule extends PathfinderModule {
    List<BlockBehaviorResolver> resolvers();
}
