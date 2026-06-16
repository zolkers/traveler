package dev.traveler.core.pathfinder.module.api;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;

public interface PathfinderModuleRegistry {
    List<PathfinderModule> enabled(PathfinderModuleType type);

    PathfinderModule fallback(PathfinderModuleType type);
}
