package dev.traveler.core.pathfinder.kernel.noop;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.Objects;

public final class EmptyPathfinderModule implements PathfinderModule {
    private final PathfinderModuleDescriptor descriptor;

    public EmptyPathfinderModule(PathfinderModuleType type, String id) {
        descriptor = new PathfinderModuleDescriptor(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(type, "type"),
                Integer.MAX_VALUE,
                false);
    }

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }
}
