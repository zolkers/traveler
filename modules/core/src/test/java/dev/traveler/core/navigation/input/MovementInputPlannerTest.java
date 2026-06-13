package dev.traveler.core.navigation.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
