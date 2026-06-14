package dev.traveler.core.world.navigation;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;

@FunctionalInterface
public interface SurfaceConnectionProvider {
    void addConnections(
            SurfaceTraversalContext context,
            SurfaceNode node,
            List<Connection<SurfaceNode>> connections);

    static List<SurfaceConnectionProvider> standard() {
        return List.of(
                new AdjacentSurfaceConnectionProvider(),
                new DropSurfaceConnectionProvider(),
                new JumpSurfaceConnectionProvider(),
                new ClimbSurfaceConnectionProvider());
    }
}
