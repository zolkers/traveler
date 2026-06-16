package dev.traveler.core.capability.execution.api;

import dev.traveler.core.capability.execution.spi.ControlProjectionStrategy;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.Optional;

public interface ExecutionModule extends PathfinderModule {
    boolean supportsExecution();

    Optional<ControlProjectionStrategy> controlProjectionStrategy();
}
