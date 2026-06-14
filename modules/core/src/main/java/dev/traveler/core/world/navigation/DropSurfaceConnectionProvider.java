package dev.traveler.core.world.navigation;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;

final class DropSurfaceConnectionProvider implements SurfaceConnectionProvider {
    @Override
    public void addConnections(
            SurfaceTraversalContext context,
            SurfaceNode node,
            List<Connection<SurfaceNode>> connections) {
        for (MovementDirection direction : SurfaceConnectionDirections.CARDINAL) {
            addDropConnection(context, node, direction, connections);
        }
    }

    private static void addDropConnection(
            SurfaceTraversalContext context,
            SurfaceNode node,
            MovementDirection direction,
            List<Connection<SurfaceNode>> connections) {
        if (context.capabilities().maxSafeFallDistance() <= context.capabilities().maxStepUp()) {
            return;
        }
        SurfaceNode candidate = context.surfaceNodeAt(
                context.globalXOf(node) + direction.x() * 2,
                node.blockPosition().y() - 1,
                context.globalZOf(node) + direction.z() * 2);
        if (candidate == null || !context.canReachDrop(node, candidate, direction)) {
            return;
        }
        connections.add(new Connection<>(node, candidate, context.movementCost(node, candidate)));
    }
}
