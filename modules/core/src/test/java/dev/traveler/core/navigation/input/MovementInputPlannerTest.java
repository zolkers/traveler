package dev.traveler.core.navigation.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.steering.SteeringPlan;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import org.junit.jupiter.api.Test;

class MovementInputPlannerTest {
    private final MovementInputPlanner planner = new MovementInputPlanner(MovementInputSettings.standard());

    @Test
    void mapsWorldTargetToCameraRelativeForwardAndRightKeys() {
        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(-4.0, 64.0, 6.0),
                0.0,
                MovementIntent.idle());

        assertEquals(new MovementIntent(true, false, false, true, false, true), intent);
    }

    @Test
    void mapsSideCorrectionWithoutForcingForwardKey() {
        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(4.0, 64.0, 0.0),
                0.0,
                MovementIntent.idle());

        assertEquals(new MovementIntent(false, false, true, false, false, false), intent);
    }

    @Test
    void keepsPressedKeysInsideReleaseDeadzoneToAvoidInputFlicker() {
        MovementInputSettings settings = new MovementInputSettings(0.35, 0.2);
        MovementInputPlanner stablePlanner = new MovementInputPlanner(settings);
        MovementIntent previous = new MovementIntent(true, false, false, false, false, true);

        MovementIntent intent = stablePlanner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 0.25),
                0.0,
                previous);

        assertEquals(previous, intent);
    }

    @Test
    void releasesKeysAfterReleaseDeadzone() {
        MovementInputSettings settings = new MovementInputSettings(0.35, 0.2);
        MovementInputPlanner stablePlanner = new MovementInputPlanner(settings);
        MovementIntent previous = new MovementIntent(true, false, false, false, false, true);

        MovementIntent intent = stablePlanner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 0.05),
                0.0,
                previous);

        assertEquals(MovementIntent.idle(), intent);
    }

    @Test
    void jumpsWhenTargetIsAboveWithNoHorizontalDelta() {
        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 0.0),
                0.0,
                MovementIntent.idle());

        assertEquals(new MovementIntent(false, false, false, false, true, false), intent);
    }

    @Test
    void jumpsWhileKeepingForwardMotionForJumpPlan() {
        NavigationPoint target = new NavigationPoint(0.0, 65.0, 1.0);

        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                target,
                0.0,
                MovementIntent.idle(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill());

        assertEquals(new MovementIntent(true, false, false, false, true, true), intent);
    }

    @Test
    void usesRecoveryInputWhenGroundedAndHorizontallyBlocked() {
        NavigationPoint target = new NavigationPoint(0.0, 64.0, 4.0);

        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                target,
                0.0,
                new MovementIntent(true, false, false, false, false, true),
                LocomotionPlan.recover(),
                new AgentMotionState(true, true, new HorizontalVector(0.0, 0.01), 0.0));

        assertEquals(new MovementIntent(false, true, true, false, false, false), intent);
    }

    @Test
    void doesNotJumpForStepUpPlan() {
        NavigationPoint target = new NavigationPoint(0.0, 64.5, 1.0);

        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                target,
                0.0,
                MovementIntent.idle(),
                LocomotionPlan.stepUp(),
                AgentMotionState.groundedStill());

        assertEquals(new MovementIntent(true, false, false, false, false, true), intent);
    }

    @Test
    void doesNotPulseJumpWhileAirborne() {
        NavigationPoint target = new NavigationPoint(0.0, 65.0, 0.0);

        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 0.0),
                target,
                0.0,
                MovementIntent.idle(),
                LocomotionPlan.jump(),
                new AgentMotionState(false, false, new HorizontalVector(0.0, 0.0), -0.1));

        assertEquals(MovementIntent.idle(), intent);
    }

    @Test
    void combinesPathTangentAndLateralCorrectionForLineHoldingStrafe() {
        SteeringPlan steering = SteeringPlan.corridor(
                new NavigationPoint(-0.65, 64.0, 4.0),
                new NavigationPoint(0.0, 64.0, 4.0),
                new NavigationPoint(0.0, 64.0, 2.0),
                new HorizontalVector(0.0, 1.0),
                new HorizontalVector(-0.65, 0.0),
                1.0,
                2.0,
                true);

        MovementIntent intent = planner.plan(
                new NavigationPoint(1.0, 64.0, 2.0),
                steering,
                0.0,
                MovementIntent.idle(),
                LocomotionPlan.walk(),
                AgentMotionState.groundedStill());

        assertEquals(new MovementIntent(true, false, false, true, false, true), intent);
    }

    @Test
    void recentersWithPureStrafeWhenLateralErrorDominatesCorridorFollow() {
        SteeringPlan steering = SteeringPlan.corridor(
                new NavigationPoint(-2.0, 64.0, 4.0),
                new NavigationPoint(0.0, 64.0, 4.0),
                new NavigationPoint(0.0, 64.0, 2.0),
                new HorizontalVector(0.0, 1.0),
                new HorizontalVector(-2.0, 0.0),
                2.0,
                2.0,
                true);

        MovementIntent intent = planner.plan(
                new NavigationPoint(2.0, 64.0, 2.0),
                steering,
                0.0,
                MovementIntent.idle(),
                LocomotionPlan.walk(),
                AgentMotionState.groundedStill());

        assertEquals(new MovementIntent(false, false, false, true, false, false), intent);
    }

    @Test
    void recentersBeforeTriggeringSpecialJumpAction() {
        SteeringPlan steering = SteeringPlan.corridor(
                new NavigationPoint(-2.0, 65.0, 4.0),
                new NavigationPoint(0.0, 65.0, 4.0),
                new NavigationPoint(0.0, 64.0, 2.0),
                new HorizontalVector(0.0, 1.0),
                new HorizontalVector(-2.0, 0.0),
                2.0,
                2.0,
                true);

        MovementIntent intent = planner.plan(
                new NavigationPoint(2.0, 64.0, 2.0),
                steering,
                0.0,
                MovementIntent.idle(),
                LocomotionPlan.jump(),
                AgentMotionState.groundedStill());

        assertEquals(new MovementIntent(false, false, false, true, false, false), intent);
    }

    @Test
    void backpedalsWhenSteeringTargetIsBehindEndOfPath() {
        SteeringPlan steering = SteeringPlan.corridor(
                new NavigationPoint(0.0, 64.0, 4.0),
                new NavigationPoint(0.0, 64.0, 4.0),
                new NavigationPoint(0.0, 64.0, 4.0),
                new HorizontalVector(0.0, 1.0),
                new HorizontalVector(0.0, 0.0),
                0.0,
                4.0,
                false);

        MovementIntent intent = planner.plan(
                new NavigationPoint(0.0, 64.0, 6.0),
                steering,
                0.0,
                MovementIntent.idle(),
                LocomotionPlan.walk(),
                AgentMotionState.groundedStill());

        assertEquals(new MovementIntent(false, true, false, false, false, false), intent);
    }
}
