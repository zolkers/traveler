package dev.traveler.core.pathfinder.module.api;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.List;
import java.util.Objects;

public record PathfinderModuleSelection(List<PathfinderModule> enabled, List<PathfinderModule> disabled) {
    public PathfinderModuleSelection {
        enabled = List.copyOf(Objects.requireNonNull(enabled, "enabled"));
        disabled = List.copyOf(Objects.requireNonNull(disabled, "disabled"));
    }
}
