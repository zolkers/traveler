package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;

public interface SurfaceTraversalContext {
    int globalXOf(SurfaceNode node);

    int globalZOf(SurfaceNode node);

    SurfaceNode surfaceNodeAt(int globalX, int blockY, int globalZ);

    boolean canReach(SurfaceNode from, SurfaceNode to, MovementDirection direction);

    boolean canReachDrop(SurfaceNode from, SurfaceNode to, MovementDirection direction);

    boolean canReachJump(SurfaceNode from, SurfaceNode to, MovementDirection direction);

    MovementCapabilities capabilities();

    double movementCost(SurfaceNode from, SurfaceNode to);
}
