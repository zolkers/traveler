package dev.traveler.core.world.behavior.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static dev.traveler.core.world.behavior.special.SurfaceMovementTestSupport.context;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpecialBlockBehaviorTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, true, 0.6, 1.25, 3.0);
    private static final MovementCapabilities SWIM_ONLY =
            new MovementCapabilities(false, true, false, false, 0.0, 0.0, 0.0);

    @Test
    void carpetUsesThinSurfaceWalkingRules() {
        MovementDecision decision = new CarpetBlockBehavior()
                .evaluateMovement(context(node(64.0), node(64.0625), MovementDirection.north(), PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.STEP_UP, decision.action());
    }

    @Test
    void ladderIsClimbableButNotAStandingSurface() {
        LadderBlockBehavior behavior = new LadderBlockBehavior(HorizontalFacing.SOUTH);

        assertFalse(behavior.supportsStanding(PLAYER));
        assertFalse(behavior.allowsRouteSmoothing(PLAYER));
        assertTrue(behavior.preservesRouteGeometry(PLAYER));
        assertEquals(Set.of(HorizontalFacing.SOUTH), behavior.climbableFaces());
        assertEquals(HorizontalFacing.SOUTH, behavior.facing());

        MovementDecision decision =
                behavior.evaluateMovement(context(node(63.0), node(64.0), MovementDirection.south(), PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.CLIMB, decision.action());
    }

    @Test
    void ladderClimbSurfaceControlsNavigationNodeAndTarget() {
        LadderBlockBehavior behavior = new LadderBlockBehavior(HorizontalFacing.WEST);
        BlockPosition ladder = new BlockPosition(1, 64, 0);

        ClimbSurfaceGeometry surface = behavior.climbSurface(HorizontalFacing.WEST, PLAYER).orElseThrow();

        assertEquals(new SurfaceNode(ladder, 0, 1, 64.0), surface.node(ladder));
        assertEquals(new WorldPoint(1.3, 64.0, 0.5), surface.target(ladder, 64.0));
        assertTrue(behavior.climbSurface(HorizontalFacing.EAST, PLAYER).isEmpty());
    }

    @Test
    void fenceAndWallAreTallObstaclesInsteadOfWalkableSurfaces() {
        FenceBlockBehavior fence = new FenceBlockBehavior();
        WallBlockBehavior wall = new WallBlockBehavior();

        assertFalse(fence.supportsStanding(PLAYER));
        assertFalse(wall.supportsStanding(PLAYER));
        assertEquals(MovementAction.BLOCKED,
                fence.evaluateMovement(context(node(64.0), node(65.5), MovementDirection.east(), PLAYER)).action());
        assertEquals(MovementAction.BLOCKED,
                wall.evaluateMovement(context(node(64.0), node(65.5), MovementDirection.east(), PLAYER)).action());
    }

    @Test
    void vineKeepsAttachmentFacesAndCeilingAttachment() {
        VineBlockBehavior behavior =
                new VineBlockBehavior(Set.of(HorizontalFacing.NORTH, HorizontalFacing.EAST), true);

        assertFalse(behavior.supportsStanding(PLAYER));
        assertFalse(behavior.allowsRouteSmoothing(PLAYER));
        assertTrue(behavior.preservesRouteGeometry(PLAYER));
        assertEquals(Set.of(HorizontalFacing.SOUTH, HorizontalFacing.WEST), behavior.climbableFaces());
        assertEquals(Set.of(HorizontalFacing.NORTH, HorizontalFacing.EAST), behavior.attachedFaces());
        assertTrue(behavior.ceilingAttached());

        MovementDecision decision =
                behavior.evaluateMovement(context(node(63.0), node(64.0), MovementDirection.north(), PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.CLIMB, decision.action());
    }

    @Test
    void climbablesRejectProfilesWithoutWalkingControls() {
        LadderBlockBehavior ladder = new LadderBlockBehavior(HorizontalFacing.WEST);
        VineBlockBehavior vine = new VineBlockBehavior(Set.of(HorizontalFacing.WEST), false);

        assertEquals(MovementAction.BLOCKED,
                ladder.evaluateMovement(context(node(63.0), node(64.0), MovementDirection.west(), SWIM_ONLY))
                        .action());
        assertEquals(MovementAction.BLOCKED,
                vine.evaluateMovement(context(node(63.0), node(64.0), MovementDirection.west(), SWIM_ONLY))
                        .action());
        assertFalse(ladder.preservesRouteGeometry(SWIM_ONLY));
        assertFalse(vine.preservesRouteGeometry(SWIM_ONLY));
    }

    private static SurfaceNode node(double floorY) {
        return new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, floorY);
    }
}
