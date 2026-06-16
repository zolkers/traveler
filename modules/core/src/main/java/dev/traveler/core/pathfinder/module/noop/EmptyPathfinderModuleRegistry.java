package dev.traveler.core.pathfinder.module.noop;

import dev.traveler.core.pathfinder.kernel.noop.EmptyPathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.pathfinder.module.api.PathfinderModuleRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EmptyPathfinderModuleRegistry implements PathfinderModuleRegistry {
    @Override
    public List<PathfinderModule> enabled(PathfinderModuleType type) {
        Objects.requireNonNull(type, "type");
        return List.of();
    }

    @Override
    public PathfinderModule fallback(PathfinderModuleType type) {
        Objects.requireNonNull(type, "type");
        return new EmptyPathfinderModule(type, "fallback." + type.name().toLowerCase(Locale.ROOT));
    }
}
