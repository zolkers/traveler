package dev.traveler.core.navigation.locomotion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.input.MovementIntent;
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
        LocomotionDecision firstGroundFrame = sequencer.update(
                airborne.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());
        LocomotionDecision secondGroundFrame = sequencer.update(
                firstGroundFrame.state(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill(),
                MovementIntent.idle());

        assertEquals(LocomotionAction.JUMP, first.plan().action());
        assertEquals(LocomotionAction.WALK, airborne.plan().action());
        assertEquals(LocomotionAction.WALK, firstGroundFrame.plan().action());
        assertEquals(LocomotionAction.JUMP, secondGroundFrame.plan().action());
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
}
