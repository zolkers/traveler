package dev.traveler.core.route.step;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteStep;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.navigation.SurfaceClimbTraversal;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClimbSurfaceRouteStepProvider implements SurfaceRouteStepProvider {
    @Override
    public Optional<List<RouteStep>> routeSteps(SurfaceRouteStepContext context) {
        SurfaceRouteStepContext safeContext = Objects.requireNonNull(context, "context");
        if (safeContext.decision().action() != MovementAction.CLIMB) {
            return Optional.empty();
        }
        return Optional.of(climbRouteSteps(safeContext));
    }

    private static List<RouteStep> climbRouteSteps(SurfaceRouteStepContext context) {
        List<SurfaceNode> climbNodes = SurfaceClimbTraversal.climbRouteNodes(
                        context.worldLayer(),
                        context.from(),
                        context.to(),
                        context.capabilities())
                .orElseGet(List::of);
        List<RouteStep> steps = new ArrayList<>(climbNodes.size() + 1);
        SurfaceNode current = context.from();
        for (SurfaceNode climbNode : climbNodes) {
            steps.add(climbRouteStep(context, current, climbNode));
            current = climbNode;
        }
        steps.add(climbRouteStep(context, current, context.to()));
        return List.copyOf(steps);
    }

    private static RouteStep climbRouteStep(
            SurfaceRouteStepContext context,
            SurfaceNode from,
            SurfaceNode to) {
        return new RouteStep(
                from,
                to,
                MovementAction.CLIMB,
                SurfaceRouteStepContext.surfaceDistance(from, to),
                climbStepTargetPoint(context, from, to));
    }

    private static NavigationPoint climbStepTargetPoint(
            SurfaceRouteStepContext context,
            SurfaceNode from,
            SurfaceNode to) {
        if (!SurfaceClimbTraversal.isClimbable(
                context.worldLayer().surfaceBlock(to.blockPosition()),
                context.capabilities())) {
            return context.pointOf(to);
        }
        return SurfaceClimbTraversal.climbFaceTarget(
                        context.worldLayer(),
                        from,
                        to,
                        context.capabilities())
                .orElseGet(() -> context.pointOf(to));
    }
}
