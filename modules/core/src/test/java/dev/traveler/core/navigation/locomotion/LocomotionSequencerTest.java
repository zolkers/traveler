package dev.traveler.core.navigation.locomotion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import org.junit.jupiter.api.Test;

class LocomotionSequencerTest {
    private final LocomotionSequencer sequencer =
            new LocomotionSequencer(new LocomotionSequencerSettings(2, 0.08));

    @Test
    void delaysSecondJumpUntilAgentHasSettledAfterPreviousAction() {
        LocomotionDecision first = sequencer.update(
                LocomotionExecutionState.start(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        LocomotionDecision airborne = sequencer.update(
                first.state(),
                LocomotionPlan.jump(),
                airborne(),
                new MovementIntent(true, false, false, false, true, true));
        LocomotionDecision heldGroundFrame = sequencer.update(
                airborne.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                previousJumpIntent());
        LocomotionDecision heldGroundFrame2 = sequencer.update(
                heldGroundFrame.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                previousJumpIntent());
        LocomotionDecision heldGroundFrame3 = sequencer.update(
                heldGroundFrame2.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                previousJumpIntent());
        LocomotionDecision settlingGroundFrame = sequencer.update(
                heldGroundFrame3.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());
        LocomotionDecision stableGroundFrame = sequencer.update(
                settlingGroundFrame.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        assertEquals(LocomotionAction.JUMP, first.plan().action());
        assertEquals(LocomotionAction.JUMP, airborne.plan().action());
        assertEquals(LocomotionAction.JUMP, heldGroundFrame.plan().action());
        assertEquals(LocomotionAction.JUMP, heldGroundFrame2.plan().action());
        assertEquals(LocomotionAction.JUMP, heldGroundFrame3.plan().action());
        assertEquals(LocomotionAction.WALK, settlingGroundFrame.plan().action());
        assertEquals(LocomotionAction.JUMP, stableGroundFrame.plan().action());
    }

    @Test
    void holdsInitialJumpWhileAgentIsStillGroundedForRenderFrameReliability() {
        LocomotionDecision first = sequencer.update(
                LocomotionExecutionState.start(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        LocomotionDecision second = sequencer.update(
                first.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                new MovementIntent(true, false, false, false, true, true));

        assertEquals(LocomotionAction.JUMP, first.plan().action());
        assertEquals(LocomotionAction.JUMP, second.plan().action());
    }

    @Test
    void holdsInitialJumpForAShortAirborneWindow() {
        LocomotionDecision first = sequencer.update(
                LocomotionExecutionState.start(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        LocomotionDecision second = sequencer.update(
                first.state(),
                LocomotionPlan.walk(),
                airborne(),
                new MovementIntent(true, false, false, false, true, true));

        assertEquals(LocomotionAction.JUMP, first.plan().action());
        assertEquals(LocomotionAction.JUMP, second.plan().action());
    }

    @Test
    void holdsInitialJumpWhenNextRenderFrameTemporarilyRequestsWalk() {
        LocomotionDecision first = sequencer.update(
                LocomotionExecutionState.start(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        LocomotionDecision second = sequencer.update(
                first.state(),
                LocomotionPlan.walk(),
                AgentMotionState.groundedStill(),
                new MovementIntent(true, false, false, false, true, true));

        assertEquals(LocomotionAction.JUMP, first.plan().action());
        assertEquals(LocomotionAction.JUMP, second.plan().action());
    }

    @Test
    void delaysAnySpecialActionAfterDropUntilStableGroundContact() {
        LocomotionDecision drop = sequencer.update(
                LocomotionExecutionState.start(),
                LocomotionPlan.drop(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        LocomotionDecision falling = sequencer.update(
                drop.state(),
                LocomotionPlan.stepUp(),
                airborne(),
                MovementIntent.idle());
        LocomotionDecision firstGroundFrame = sequencer.update(
                falling.state(),
                LocomotionPlan.stepUp(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());
        LocomotionDecision secondGroundFrame = sequencer.update(
                firstGroundFrame.state(),
                LocomotionPlan.stepUp(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        assertEquals(LocomotionAction.DROP, drop.plan().action());
        assertEquals(LocomotionAction.WALK, falling.plan().action());
        assertEquals(LocomotionAction.WALK, firstGroundFrame.plan().action());
        assertEquals(LocomotionAction.STEP_UP, secondGroundFrame.plan().action());
    }

    private static AgentMotionState airborne() {
        return new AgentMotionState(false, false, new HorizontalVector(0.0, 0.1), -0.2);
    }

    private static MovementIntent previousJumpIntent() {
        return new MovementIntent(true, false, false, false, true, true);
    }
}
