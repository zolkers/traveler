package dev.traveler.core.world.navigation;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ClimbSurfaceConnectionProvider implements SurfaceConnectionProvider {
    private static final double FLOOR_EPSILON = 0.001;

    @Override
    public void addConnections(
            SurfaceTraversalContext context,
            SurfaceNode node,
            List<Connection<SurfaceNode>> connections) {
        Set<SurfaceNode> connected = new HashSet<>();
        for (HorizontalOffset climbOffset : HorizontalDirections.CARDINAL) {
            addColumnLandings(context, node, climbOffset, connected, connections);
        }
    }

    private static void addColumnLandings(
            SurfaceTraversalContext context,
            SurfaceNode node,
            HorizontalOffset climbOffset,
            Set<SurfaceNode> connected,
            List<Connection<SurfaceNode>> connections) {
        int climbGlobalX = context.globalXOf(node) + climbOffset.x();
        int climbGlobalZ = context.globalZOf(node) + climbOffset.z();
        if (!hasAdjacentClimbStart(context, node, climbGlobalX, climbGlobalZ)) {
            return;
        }
        for (HorizontalOffset landingOffset : HorizontalDirections.CARDINAL) {
            addLandingLine(context, node, climbGlobalX, climbGlobalZ, landingOffset, connected, connections);
        }
    }

    private static boolean hasAdjacentClimbStart(
            SurfaceTraversalContext context,
            SurfaceNode node,
            int climbGlobalX,
            int climbGlobalZ) {
        int bodyY = (int) Math.floor(node.floorY() + FLOOR_EPSILON);
        return context.hasClimbableAtGlobalCell(climbGlobalX, bodyY, climbGlobalZ)
                || context.hasClimbableAtGlobalCell(climbGlobalX, bodyY - 1, climbGlobalZ);
    }

    private static void addLandingLine(
            SurfaceTraversalContext context,
            SurfaceNode node,
            int climbGlobalX,
            int climbGlobalZ,
            HorizontalOffset landingOffset,
            Set<SurfaceNode> connected,
            List<Connection<SurfaceNode>> connections) {
        int landingGlobalX = climbGlobalX + landingOffset.x();
        int landingGlobalZ = climbGlobalZ + landingOffset.z();
        for (int supportY = context.minBlockY(); supportY <= context.maxBlockY(); supportY++) {
            addLandingAtY(context, node, landingGlobalX, landingGlobalZ, supportY, connected, connections);
        }
    }

    private static void addLandingAtY(
            SurfaceTraversalContext context,
            SurfaceNode node,
            int landingGlobalX,
            int landingGlobalZ,
            int supportY,
            Set<SurfaceNode> connected,
            List<Connection<SurfaceNode>> connections) {
        SurfaceNode candidate = context.surfaceNodeAt(landingGlobalX, supportY, landingGlobalZ);
        if (candidate == null || node.sameSubcell(candidate) || connected.contains(candidate)) {
            return;
        }
        if (!context.canReachClimb(node, candidate)) {
            return;
        }
        connected.add(candidate);
        connections.add(new Connection<>(node, candidate, context.movementCost(node, candidate)));
    }
}
