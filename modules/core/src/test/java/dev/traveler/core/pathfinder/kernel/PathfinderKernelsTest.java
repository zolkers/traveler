package dev.traveler.core.pathfinder.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.ClimbTraversalModule;
import dev.traveler.core.capability.traversal.impl.WalkTraversalModule;
import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelMode;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelResult;
import dev.traveler.core.pathfinder.kernel.impl.PathfinderKernels;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.route.RouteSearchFailureReason;
import dev.traveler.core.route.RouteSearchSettings;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementProfile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PathfinderKernelsTest {
    @Test
    void routeOnlyUsesStandardTraversalModulesInBehaviorOrder() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly();

        PathfinderKernelResult result = kernel.findRoute(
                new TestSurfaceWorldLayer(flatSurface(0, 2)),
                new BlockPosition(0, 64, 0),
                RouteGoal.blockTarget(new BlockPosition(2, 63, 0)),
                standardProfile());

        assertEquals(PathfinderKernelMode.ROUTE_ONLY, kernel.mode());
        assertTrue(result.routePlan().isPresent());
        assertFalse(result.routePlan().orElseThrow().segments().isEmpty());
        assertEquals(
                List.of("traversal.climb", "traversal.swim", "traversal.walk", "traversal.drop", "traversal.jump"),
                activeModuleIds(result));
    }

    @Test
    void routeOnlyOrdersCustomTraversalModulesByPriority() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.of(
                new WalkTraversalModule(),
                new ClimbTraversalModule()));

        PathfinderKernelResult result = kernel.findRoute(
                new TestSurfaceWorldLayer(flatSurface(0, 2)),
                new BlockPosition(0, 64, 0),
                RouteGoal.blockTarget(new BlockPosition(2, 63, 0)),
                standardProfile());

        assertTrue(result.routePlan().isPresent());
        assertEquals(List.of("traversal.climb", "traversal.walk"), activeModuleIds(result));
    }

    @Test
    void routeOnlyWithNoTraversalModuleFailsThroughRouteDiagnostics() {
        PathfinderKernel kernel =
                PathfinderKernels.routeOnly(List.of(new NoTraversalModule("traversal.none")));

        PathfinderKernelResult result = kernel.findRoute(
                new TestSurfaceWorldLayer(flatSurface(0, 2)),
                new BlockPosition(0, 64, 0),
                RouteGoal.blockTarget(new BlockPosition(2, 63, 0)),
                standardProfile());

        assertEquals(PathfinderKernelMode.ROUTE_ONLY, kernel.mode());
        assertTrue(result.routePlan().isEmpty());
        assertEquals(RouteSearchFailureReason.NO_START_SURFACE, result.diagnostics().reason());
        assertTrue(result.activeModules().isEmpty());
    }

    @Test
    void routeOnlyExcludesDisabledTraversalModulesFromActiveDescriptors() {
        PathfinderKernel kernel = PathfinderKernels.routeOnly(List.of(
                new NoTraversalModule("traversal.none"),
                new WalkTraversalModule()));

        PathfinderKernelResult result = kernel.findRoute(
                new TestSurfaceWorldLayer(flatSurface(0, 2)),
                new BlockPosition(0, 64, 0),
                RouteGoal.blockTarget(new BlockPosition(2, 63, 0)),
                standardProfile());

        assertTrue(result.routePlan().isPresent());
        assertEquals(List.of("traversal.walk"), activeModuleIds(result));
    }

    @Test
    void routeOnlyRejectsNullTraversalModules() {
        assertThrows(
                NullPointerException.class,
                () -> PathfinderKernels.routeOnly((List<TraversalModule>) null));
        assertThrows(
                NullPointerException.class,
                () -> PathfinderKernels.routeOnly(Arrays.asList(new WalkTraversalModule(), null)));
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
