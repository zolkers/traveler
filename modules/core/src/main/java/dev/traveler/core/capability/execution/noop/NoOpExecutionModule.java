package dev.traveler.core.capability.execution.noop;

import dev.traveler.core.capability.execution.api.ExecutionModule;
import dev.traveler.core.capability.execution.spi.ControlProjectionStrategy;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.Optional;

public final class NoOpExecutionModule implements ExecutionModule {
    private final PathfinderModuleDescriptor descriptor =
            new PathfinderModuleDescriptor(
                    "execution.none",
                    PathfinderModuleType.EXECUTION,
                    Integer.MAX_VALUE,
                    false);

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public boolean supportsExecution() {
        return false;
    }

    @Override
    public Optional<ControlProjectionStrategy> controlProjectionStrategy() {
        return Optional.empty();
    }
}
