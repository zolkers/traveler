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
import org.junit.jupiter.api.Test;

class StairBlockBehaviorTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);
    private static final MovementCapabilities LOW_JUMP_PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 0.4, 3.0);

    @Test
    void frontApproachStepsUpNorthFacingStairWithoutJumping() {
        StairBlockBehavior behavior = new StairBlockBehavior(HorizontalFacing.NORTH);

        MovementDecision decision = behavior.evaluateMovement(context(
                node(1, 0, 63.5), node(1, 1, 64.0), MovementDirection.south(), LOW_JUMP_PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.STEP_UP, decision.action());
    }

    @Test
    void sideApproachRequiresJumpOntoRaisedStairSurface() {
        StairBlockBehavior behavior = new StairBlockBehavior(HorizontalFacing.NORTH);

        MovementDecision decision = behavior.evaluateMovement(context(
                node(0, 1, 63.5), node(1, 1, 64.0), MovementDirection.east(), PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.JUMP, decision.action());
    }

    @Test
    void sideApproachRejectsRaisedStairSurfaceWhenJumpIsTooLow() {
        StairBlockBehavior behavior = new StairBlockBehavior(HorizontalFacing.NORTH);

        MovementDecision decision = behavior.evaluateMovement(context(
                node(0, 1, 63.5), node(1, 1, 64.0), MovementDirection.east(), LOW_JUMP_PLAYER));

        assertFalse(decision.allowed());
        assertEquals(MovementAction.BLOCKED, decision.action());
    }

    @Test
    void backApproachUsesFullBlockJumpRules() {
        StairBlockBehavior behavior = new StairBlockBehavior(HorizontalFacing.NORTH);

        MovementDecision decision = behavior.evaluateMovement(context(
                node(1, 1, 63.0), node(1, 0, 64.0), MovementDirection.north(), PLAYER));

        assertTrue(decision.allowed());
        assertEquals(MovementAction.JUMP, decision.action());
    }

    private static SurfaceMovementContext context(
            SurfaceNode from,
            SurfaceNode to,
            MovementDirection direction,
            MovementCapabilities capabilities) {
        return new SurfaceMovementContext(from, to, capabilities, direction);
    }

    private static SurfaceNode node(int cellX, int cellZ, double floorY) {
        return new SurfaceNode(new BlockPosition(0, 63, 0), cellX, cellZ, floorY);
    }
}
