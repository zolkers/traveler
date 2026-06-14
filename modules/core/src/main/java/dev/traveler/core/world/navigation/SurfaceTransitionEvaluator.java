package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public final class SurfaceTransitionEvaluator {
    private final MovementCapabilities capabilities;
    private final SurfaceMovementEvaluator surfaceMovementEvaluator;

    public SurfaceTransitionEvaluator(MovementCapabilities capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.surfaceMovementEvaluator = new SurfaceMovementEvaluator(this.capabilities);
    }

    public MovementDecision decision(SurfaceWorldLayer worldLayer, SurfaceNode from, SurfaceNode to) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        if (SurfaceClimbTraversal.canClimb(layer, from, safeTo, capabilities)) {
            return MovementDecision.climb();
        }
        SurfaceBlock destinationBlock = layer.surfaceBlock(safeTo.blockPosition());
        MovementDecision surfaceDecision = surfaceMovementEvaluator.decision(from, safeTo, destinationBlock);
        return surfaceDecision;
    }
}
