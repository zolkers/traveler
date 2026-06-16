package dev.traveler.core.world.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.api.BehaviorTag;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.CollisionSemantics;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.api.TraversalAffordance;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.behavior.special.FluidBlockBehavior;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.layer.SurfaceWorldLayer;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockSemanticsTest {
    private static final MovementCapabilities WALKER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);
    private static final MovementCapabilities SWIMMER =
            new MovementCapabilities(true, true, false, false, 0.6, 1.25, 3.0);
    private static final MovementCapabilities LOW_JUMP_WALKER =
            new MovementCapabilities(true, false, false, false, 0.6, 0.4, 3.0);

    @Test
    void fullBlockDescribesStableWalkableSupportAndKeepsCompatibilityDecision() {
        FullBlockBehavior behavior = new FullBlockBehavior();
        SurfaceMovementContext context = context(
                node(0, 0, 63.0),
                node(1, 0, 63.5),
                solidBlock(behavior),
                solidBlock(behavior),
                MovementDirection.east(),
                WALKER);

        BlockSemantics semantics = behavior.describe(context);

        assertEquals(CollisionSemantics.SOLID, semantics.collision());
        assertEquals(SupportSemantics.STANDABLE, semantics.support());
        assertEquals(FluidSemantics.NONE, semantics.fluid());
        assertEquals(Set.of(TraversalAffordance.STEP_UP), semantics.affordances());
        assertTrue(behavior.supportsStanding(WALKER));
        assertEquals(MovementAction.STEP_UP, behavior.evaluateMovement(context).action());
    }

    @Test
    void stairApproachSemanticsDescribeRaisedEntryWithoutEmbeddingLegacyActions() {
        StairBlockBehavior behavior = new StairBlockBehavior(HorizontalFacing.NORTH);

        BlockSemantics frontApproach = behavior.describe(context(
                node(1, 0, 63.5),
                node(1, 1, 64.0),
                solidBlock(new FullBlockBehavior()),
                solidBlock(behavior),
                MovementDirection.south(),
                LOW_JUMP_WALKER));
        BlockSemantics sideApproach = behavior.describe(context(
                node(0, 1, 63.5),
                node(1, 1, 64.0),
                solidBlock(new FullBlockBehavior()),
                solidBlock(behavior),
                MovementDirection.east(),
                WALKER));

        assertEquals(Set.of(TraversalAffordance.STEP_UP), frontApproach.affordances());
        assertEquals(Set.of(TraversalAffordance.JUMP), sideApproach.affordances());
        assertEquals(MovementAction.STEP_UP,
                behavior.evaluateMovement(context(
                                node(1, 0, 63.5),
                                node(1, 1, 64.0),
                                solidBlock(new FullBlockBehavior()),
                                solidBlock(behavior),
                                MovementDirection.south(),
                                LOW_JUMP_WALKER))
                        .action());
        assertEquals(MovementAction.JUMP,
                behavior.evaluateMovement(context(
                                node(0, 1, 63.5),
                                node(1, 1, 64.0),
                                solidBlock(new FullBlockBehavior()),
                                solidBlock(behavior),
                                MovementDirection.east(),
                                WALKER))
                        .action());
    }

    @Test
    void waterIsSwimmableButDoesNotDescribeRaisedExitSupport() {
        FluidBlockBehavior water = new FluidBlockBehavior();
        FullBlockBehavior exit = new FullBlockBehavior();
        SurfaceMovementContext swimLane = context(
                node(0, 0, 64.0),
                node(1, 0, 64.0),
                fluidBlock(),
                fluidBlock(),
                MovementDirection.east(),
                SWIMMER);
        SurfaceMovementContext raisedExit = context(
                node(0, 0, 64.0),
                node(1, 0, 65.0),
                fluidBlock(),
                solidBlock(exit),
                MovementDirection.east(),
                SWIMMER);

        BlockSemantics semantics = water.describe(swimLane);

        assertEquals(CollisionSemantics.PASSABLE, semantics.collision());
        assertEquals(SupportSemantics.NONE, semantics.support());
        assertEquals(FluidSemantics.SWIMMABLE, semantics.fluid());
        assertEquals(Set.of(TraversalAffordance.SWIM), semantics.affordances());
        assertFalse(semantics.tags().contains(BehaviorTag.PRESERVE_ROUTE_GEOMETRY));
        assertFalse(water.supportsStanding(SWIMMER));
        assertEquals(MovementAction.SWIM, water.evaluateMovement(swimLane).action());
        assertEquals(MovementAction.BLOCKED, exit.evaluateMovement(raisedExit).action());
    }

    @Test
    void supportCompatibilityDerivesFromSupportSemantics() {
        BlockBehavior behavior = new SemanticsOnlyBehavior(
                SupportSemantics.STANDABLE,
                FluidSemantics.NONE,
                Set.of(TraversalAffordance.WALK));

        assertTrue(behavior.supportsStanding(WALKER));
    }

    @Test
    void movementContextFluidChecksUseFluidSemanticsInsteadOfLegacyKeys() {
        BlockBehavior semanticFluid = new SemanticsOnlyBehavior(
                SupportSemantics.NONE,
                FluidSemantics.SWIMMABLE,
                Set.of(TraversalAffordance.SWIM));
        SurfaceMovementContext context = context(
                node(0, 0, 64.0),
                node(1, 0, 64.0),
                passableBlock(semanticFluid, FluidHandling.AVOID),
                passableBlock(new FullBlockBehavior(), FluidHandling.AVOID),
                MovementDirection.east(),
                SWIMMER);

        assertTrue(context.startsInFluid());
        assertFalse(context.endsInFluid());
    }

    @Test
    void surfaceTraversalGraphUsesSupportSemanticsForStandableNodes() {
        BlockBehavior semanticSupport = new SemanticsOnlyBehavior(
                SupportSemantics.STANDABLE,
                FluidSemantics.NONE,
                Set.of(TraversalAffordance.WALK));
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                new BlockPosition(0, 63, 0),
                solidBlock(semanticSupport)));
        SurfaceNode anchor = new SurfaceNode(new BlockPosition(0, 63, 0), 0, 0, 64.0);
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, anchor, anchor, WALKER, 1, 1);

        SurfaceNode surface = graph.surfaceNodeAt(0, 63, 0);

        assertEquals(anchor, surface);
    }

    private static SurfaceMovementContext context(
            SurfaceNode from,
            SurfaceNode to,
            SurfaceBlock fromBlock,
            SurfaceBlock toBlock,
            MovementDirection direction,
            MovementCapabilities capabilities) {
        return new SurfaceMovementContext(from, to, fromBlock, toBlock, capabilities, direction);
    }

    private static SurfaceNode node(int cellX, int cellZ, double floorY) {
        return new SurfaceNode(new BlockPosition(0, 63, 0), cellX, cellZ, floorY);
    }

    private static SurfaceBlock solidBlock(BlockBehavior behavior) {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID),
                BlockShape.fullCube(),
                behavior);
    }

    private static SurfaceBlock fluidBlock() {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.PASSABLE, FluidHandling.ALLOW),
                BlockShape.empty(),
                new FluidBlockBehavior());
    }

    private static SurfaceBlock passableBlock(BlockBehavior behavior, FluidHandling fluidHandling) {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.PASSABLE, fluidHandling),
                BlockShape.empty(),
                behavior);
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }

    private static final class SemanticsOnlyBehavior implements BlockBehavior {
        private final SupportSemantics support;
        private final FluidSemantics fluid;
        private final Set<TraversalAffordance> affordances;

        private SemanticsOnlyBehavior(
                SupportSemantics support,
                FluidSemantics fluid,
                Set<TraversalAffordance> affordances) {
            this.support = support;
            this.fluid = fluid;
            this.affordances = affordances;
        }

        @Override
        public BlockBehaviorKey key() {
            return BlockBehaviorKey.AIR;
        }

        @Override
        public SupportSemantics supportSemantics(MovementCapabilities capabilities) {
            return support;
        }

        @Override
        public FluidSemantics fluidSemantics() {
            return fluid;
        }

        @Override
        public BlockSemantics describe(SurfaceMovementContext context) {
            return BlockSemantics.of(
                    support == SupportSemantics.STANDABLE
                            ? CollisionSemantics.SOLID
                            : CollisionSemantics.PASSABLE,
                    support,
                    fluid,
                    affordances);
        }

        @Override
        public MovementDecision evaluateMovement(SurfaceMovementContext context) {
            return BlockBehavior.adaptMovementDecision(context, describe(context));
        }
    }
}
