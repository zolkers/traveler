package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public final class SurfaceTransitionEvaluator {
    private final MovementCapabilities capabilities;
    private final SurfaceTransitionResolver resolver;

    public SurfaceTransitionEvaluator(MovementCapabilities capabilities) {
        this(
                capabilities,
                SurfaceTraversalFeatures.transitionResolver(SurfaceTraversalFeatures.standard()));
    }

    public SurfaceTransitionEvaluator(
            MovementCapabilities capabilities,
            SurfaceTransitionResolver resolver) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public MovementDecision decision(SurfaceWorldLayer worldLayer, SurfaceNode from, SurfaceNode to) {
        return resolver.decision(new SurfaceTransitionContext(
                worldLayer,
                from,
                to,
                capabilities));
    }

    public MovementDecision decision(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            SurfaceBlock destinationBlock,
            MovementDirection direction) {
        return resolver.decision(new SurfaceTransitionContext(
                worldLayer,
                from,
                to,
                capabilities,
                destinationBlock,
                direction));
    }
}
