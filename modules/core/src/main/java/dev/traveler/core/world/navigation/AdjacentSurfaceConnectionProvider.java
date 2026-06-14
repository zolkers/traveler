package dev.traveler.core.world.navigation;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;

final class AdjacentSurfaceConnectionProvider implements SurfaceConnectionProvider {
    @Override
    public void addConnections(
            SurfaceTraversalContext context,
            SurfaceNode node,
            List<Connection<SurfaceNode>> connections) {
        for (MovementDirection direction : SurfaceConnectionDirections.EIGHT_WAY) {
            addDirectionConnections(context, node, direction, connections);
        }
    }

    private static void addDirectionConnections(
            SurfaceTraversalContext context,
            SurfaceNode node,
            MovementDirection direction,
            List<Connection<SurfaceNode>> connections) {
        int destinationX = context.globalXOf(node) + direction.x();
        int destinationZ = context.globalZOf(node) + direction.z();
        for (int yOffset : SurfaceSearchOffsets.SUPPORT_Y) {
            addConnectionForCandidateY(context, node, direction, connections, destinationX, destinationZ, yOffset);
        }
    }

    private static void addConnectionForCandidateY(
            SurfaceTraversalContext context,
            SurfaceNode node,
            MovementDirection direction,
            List<Connection<SurfaceNode>> connections,
            int destinationX,
            int destinationZ,
            int yOffset) {
        SurfaceNode candidate =
                context.surfaceNodeAt(destinationX, node.blockPosition().y() + yOffset, destinationZ);
        if (candidate == null || !context.canReach(node, candidate, direction)) {
            return;
        }
        connections.add(new Connection<>(node, candidate, context.movementCost(node, candidate)));
    }
}
