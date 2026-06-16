package dev.traveler.core.world.navigation;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Objects;

final class SwimSurfaceConnectionProvider implements SurfaceConnectionProvider {
    @Override
    public void addConnections(
            SurfaceTraversalContext context,
            SurfaceNode node,
            List<Connection<SurfaceNode>> connections) {
        SurfaceTraversalContext safeContext = Objects.requireNonNull(context, "context");
        SurfaceNode safeNode = Objects.requireNonNull(node, "node");
        List<Connection<SurfaceNode>> safeConnections = Objects.requireNonNull(connections, "connections");
        SurfaceNode above = safeContext.surfaceNodeAt(
                safeContext.globalXOf(safeNode),
                safeNode.blockPosition().y() + 1,
                safeContext.globalZOf(safeNode));
        if (above == null || !safeContext.canReachSwim(safeNode, above)) {
            return;
        }
        safeConnections.add(new Connection<>(safeNode, above, safeContext.movementCost(safeNode, above)));
    }
}
