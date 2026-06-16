package dev.traveler.core.capability.smoothing.api;

import dev.traveler.core.capability.smoothing.spi.SmoothingStrategy;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;

public interface SmoothingModule<N> extends PathfinderModule {
    SmoothingStrategy<N> strategy();
}
