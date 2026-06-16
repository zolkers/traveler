package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.module.api.PathfinderModuleSelection;
import java.util.List;

public final class StandardTraversalModules {
    private StandardTraversalModules() {}

    public static List<TraversalModule> modules() {
        return List.of(
                new ClimbTraversalModule(),
                new SwimTraversalModule(),
                new WalkTraversalModule(),
                new DropTraversalModule(),
                new JumpTraversalModule());
    }

    public static PathfinderModuleSelection selection() {
        List<PathfinderModule> enabled = List.copyOf(modules());
        return new PathfinderModuleSelection(enabled, List.of());
    }
}
