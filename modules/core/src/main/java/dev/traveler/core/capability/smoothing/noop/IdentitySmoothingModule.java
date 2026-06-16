package dev.traveler.core.capability.smoothing.noop;

import dev.traveler.core.capability.smoothing.api.SmoothingModule;
import dev.traveler.core.capability.smoothing.spi.SmoothingStrategy;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.Objects;

public final class IdentitySmoothingModule<N> implements SmoothingModule<N> {
    private final PathfinderModuleDescriptor descriptor =
            new PathfinderModuleDescriptor(
                    "smoothing.identity",
                    PathfinderModuleType.SMOOTHING,
                    Integer.MAX_VALUE,
                    false);
    private final SmoothingStrategy<N> strategy = path -> Objects.requireNonNull(path, "path");

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public SmoothingStrategy<N> strategy() {
        return strategy;
    }
}
