package dev.traveler.core.route.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.climb.spi.ClimbContactResolver;
import dev.traveler.core.capability.traversal.climb.spi.ClimbTargetProjector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.route.RouteStep;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfiles;
import dev.traveler.core.world.navigation.SurfaceTransitionEvaluator;
import dev.traveler.core.world.navigation.SurfaceTransitionResolver;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClimbSurfaceRouteStepProviderTest {
    private static final WorldPoint CUSTOM_TARGET = new WorldPoint(7.25, 72.5, -3.75);

    @Test
    void injectedResolverAndProjectorDriveClimbSteps() {
        SurfaceNode from = new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, 64.0);
        SurfaceNode intermediate = new SurfaceNode(new BlockPosition(1, 64, 0), 1, 1, 65.0);
        SurfaceNode to = new SurfaceNode(new BlockPosition(1, 65, 0), 1, 1, 66.0);
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(java.util.Map.of(
                intermediate.blockPosition(), ladder(),
                to.blockPosition(), ladder()));
        FakeContactResolver contactResolver = new FakeContactResolver(world, from, to, List.of(intermediate));
        FakeTargetProjector targetProjector = new FakeTargetProjector(
                world,
                List.of(from, intermediate),
                List.of(intermediate, to));
        ClimbSurfaceRouteStepProvider provider =
                new ClimbSurfaceRouteStepProvider(contactResolver, targetProjector);

        Optional<List<RouteStep>> steps = provider.routeSteps(climbContext(world, from, to));

        assertTrue(steps.isPresent());
        assertEquals(List.of(from, intermediate), steps.orElseThrow().stream().map(RouteStep::from).toList());
        assertEquals(List.of(intermediate, to), steps.orElseThrow().stream().map(RouteStep::to).toList());
        assertEquals(List.of(MovementAction.CLIMB, MovementAction.CLIMB),
                steps.orElseThrow().stream().map(RouteStep::action).toList());
        assertEquals(List.of(CUSTOM_TARGET, CUSTOM_TARGET),
                steps.orElseThrow().stream().map(RouteStep::targetPoint).toList());
        assertEquals(1, contactResolver.calls());
        assertEquals(2, targetProjector.targetCalls());
    }

    @Test
    void rejectsNullConstructorArgs() {
        ClimbContactResolver contactResolver = (worldLayer, from, to, capabilities) -> Optional.of(List.of());
        ClimbTargetProjector targetProjector =
                (worldLayer, from, to, capabilities) -> Optional.of(CUSTOM_TARGET);

        assertThrows(NullPointerException.class, () -> new ClimbSurfaceRouteStepProvider(null, targetProjector));
        assertThrows(NullPointerException.class, () -> new ClimbSurfaceRouteStepProvider(contactResolver, null));
    }

    private static SurfaceRouteStepContext climbContext(SurfaceWorldLayer world, SurfaceNode from, SurfaceNode to) {
        return new SurfaceRouteStepContext(
                world,
                from,
                to,
                MovementProfiles.defaultPlayer(),
                new SurfaceTransitionEvaluator(
                        MovementProfiles.defaultPlayerCapabilities(),
                        new SurfaceTransitionResolver(List.of(context -> Optional.of(MovementDecision.climb())))));
    }

    private static final class FakeContactResolver implements ClimbContactResolver {
        private final SurfaceWorldLayer expectedWorldLayer;
        private final SurfaceNode expectedFrom;
        private final SurfaceNode expectedTo;
        private final List<SurfaceNode> climbNodes;
        private int calls;

        private FakeContactResolver(
                SurfaceWorldLayer expectedWorldLayer,
                SurfaceNode expectedFrom,
                SurfaceNode expectedTo,
                List<SurfaceNode> climbNodes) {
            this.expectedWorldLayer = expectedWorldLayer;
            this.expectedFrom = expectedFrom;
            this.expectedTo = expectedTo;
            this.climbNodes = List.copyOf(climbNodes);
        }

        @Override
        public Optional<List<SurfaceNode>> climbRouteNodes(
                SurfaceWorldLayer worldLayer,
                SurfaceNode from,
                SurfaceNode to,
                MovementCapabilities capabilities) {
            calls++;
            assertSame(expectedWorldLayer, worldLayer);
            assertEquals(expectedFrom, from);
            assertEquals(expectedTo, to);
            assertEquals(MovementProfiles.defaultPlayerCapabilities(), capabilities);
            return Optional.of(climbNodes);
        }

        private int calls() {
            return calls;
        }
    }

    private static final class FakeTargetProjector implements ClimbTargetProjector {
        private final SurfaceWorldLayer expectedWorldLayer;
        private final List<SurfaceNode> expectedFromNodes;
        private final List<SurfaceNode> expectedToNodes;
        private int targetCalls;

        private FakeTargetProjector(
                SurfaceWorldLayer expectedWorldLayer,
                List<SurfaceNode> expectedFromNodes,
                List<SurfaceNode> expectedToNodes) {
            this.expectedWorldLayer = expectedWorldLayer;
            this.expectedFromNodes = List.copyOf(expectedFromNodes);
            this.expectedToNodes = List.copyOf(expectedToNodes);
        }

        @Override
        public Optional<WorldPoint> climbFaceTarget(
                SurfaceWorldLayer worldLayer,
                SurfaceNode from,
                SurfaceNode to,
                MovementCapabilities capabilities) {
            assertSame(expectedWorldLayer, worldLayer);
            assertEquals(expectedFromNodes.get(targetCalls), from);
            assertEquals(expectedToNodes.get(targetCalls), to);
            assertEquals(MovementProfiles.defaultPlayerCapabilities(), capabilities);
            targetCalls++;
            return Optional.of(CUSTOM_TARGET);
        }

        private int targetCalls() {
            return targetCalls;
        }
    }

    private static SurfaceBlock ladder() {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID),
                BlockShape.empty(),
                new LadderBlockBehavior(HorizontalFacing.EAST));
    }

    private record TestSurfaceWorldLayer(java.util.Map<BlockPosition, SurfaceBlock> blocks)
            implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
