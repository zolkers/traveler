package dev.traveler.core.pathfinder.module.impl;

import dev.traveler.core.pathfinder.kernel.noop.EmptyPathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.pathfinder.module.api.PathfinderModuleRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class DefaultPathfinderModuleRegistry implements PathfinderModuleRegistry {
    private final List<PathfinderModule> modules;

    public DefaultPathfinderModuleRegistry(List<? extends PathfinderModule> modules) {
        this.modules = List.copyOf(Objects.requireNonNull(modules, "modules"));
    }

    @Override
    public List<PathfinderModule> enabled(PathfinderModuleType type) {
        Objects.requireNonNull(type, "type");
        return modules.stream()
                .filter(module -> module.descriptor().type() == type)
                .filter(module -> module.descriptor().enabled())
                .sorted(Comparator.comparingInt(module -> module.descriptor().priority()))
                .toList();
    }

    @Override
    public PathfinderModule fallback(PathfinderModuleType type) {
        Objects.requireNonNull(type, "type");
        return new EmptyPathfinderModule(type, "fallback." + type.name().toLowerCase(Locale.ROOT));
    }
}
