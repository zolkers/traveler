package dev.traveler.core.pathfinder.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.impl.WalkTraversalModule;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelResult;
import dev.traveler.core.pathfinder.kernel.impl.PathfinderKernels;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.route.RouteSearchFailureReason;
import dev.traveler.core.route.RouteSearchSettings;
import dev.traveler.core.route.api.RoutePlan;
import dev.traveler.core.route.api.RouteTraversalHint;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PathfinderKernelInterchangeabilityTest {
    @Test
    void walkOnlyKernelFindsFlatRouteWithoutClimbModule() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.of(new WalkTraversalModule()));

        PathfinderKernelResult result = findFlatRoute(kernel, 0, 2);

        RoutePlan routePlan = result.routePlan().orElseThrow();
        assertFalse(routePlan.segments().isEmpty());
        assertTrue(routePlan.segments().stream()
                .allMatch(segment -> segment.traversalHint() == RouteTraversalHint.WALK));
        assertEquals(List.of("traversal.walk"), activeModuleIds(result));
    }

    @Test
    void kernelWithoutEnabledTraversalModulesReturnsRouteDiagnostics() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.of());

        PathfinderKernelResult result = findFlatRoute(kernel, 0, 2);

        assertTrue(result.routePlan().isEmpty());
        assertEquals(RouteSearchFailureReason.NO_START_SURFACE, result.diagnostics().reason());
        assertTrue(result.activeModules().isEmpty());
    }

    private static PathfinderKernelResult findFlatRoute(PathfinderKernel kernel, int minX, int maxX) {
        return kernel.findRoute(
                new TestSurfaceWorldLayer(flatSurface(minX, maxX)),
                new BlockPosition(minX, 64, 0),
                RouteGoal.blockTarget(new BlockPosition(maxX, 63, 0)),
                standardProfile());
    }

    private static List<String> activeModuleIds(PathfinderKernelResult result) {
        return result.activeModules().stream()
                .map(descriptor -> descriptor.id())
                .toList();
    }

    private static MovementProfile standardProfile() {
        return RouteSearchSettings.standardClient().movementProfile();
    }

    private static Map<BlockPosition, SurfaceBlock> flatSurface(int minX, int maxX) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            blocks.put(new BlockPosition(x, 63, 0), fullBlock());
        }
        return blocks;
    }

    private static SurfaceBlock fullBlock() {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID),
                BlockShape.fullCube(),
                new FullBlockBehavior());
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
