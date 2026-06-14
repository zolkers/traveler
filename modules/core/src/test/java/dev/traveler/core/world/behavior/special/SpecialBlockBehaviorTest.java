package dev.traveler.core.world.behavior.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
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
    void barrierUsesFullSolidWalkingRules() {
        MovementDecision decision = new BarrierBlockBehavior()
                .evaluateMovement(context(node(63.0), node(64.0), MovementDirection.east(), PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.JUMP, decision.action());
    }

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
        assertEquals(HorizontalFacing.SOUTH, behavior.facing());

        MovementDecision decision =
                behavior.evaluateMovement(context(node(63.0), node(64.0), MovementDirection.south(), PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.CLIMB, decision.action());
    }

    @Test
    void vineKeepsAttachmentFacesAndCeilingAttachment() {
        VineBlockBehavior behavior =
                new VineBlockBehavior(Set.of(HorizontalFacing.NORTH, HorizontalFacing.EAST), true);

        assertFalse(behavior.supportsStanding(PLAYER));
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
    }

    private static SurfaceMovementContext context(
            SurfaceNode from,
            SurfaceNode to,
            MovementDirection direction,
            MovementCapabilities capabilities) {
        return new SurfaceMovementContext(from, to, capabilities, direction);
    }

    private static SurfaceNode node(double floorY) {
        return new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, floorY);
    }
}
