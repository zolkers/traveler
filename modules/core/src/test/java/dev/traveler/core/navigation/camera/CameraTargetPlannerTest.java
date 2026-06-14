package dev.traveler.core.navigation.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class CameraTargetPlannerTest {
    @Test
    void looksAheadPastVerticalActionNodesWithDampedPitch() {
        CameraTargetPlanner planner = new CameraTargetPlanner(new CameraTargetSettings(3.5, 0.35, 0.0));
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 0.0),
                new NavigationPoint(0.0, 65.0, 8.0)));

        CameraAngles target = planner.targetAngles(
                path,
                new NavigationPoint(0.0, 64.0, 0.0),
                PathProgress.start(),
                new CameraAngles(90.0, 12.0));

        assertEquals(0.0, target.yawDegrees());
        assertTrue(target.pitchDegrees() < 0.0);
        assertTrue(target.pitchDegrees() > -18.0);
    }

    @Test
    void keepsYawWhenOnlyVerticalActionHasNoHorizontalDirection() {
        CameraTargetPlanner planner = new CameraTargetPlanner(new CameraTargetSettings(3.5, 0.35, 0.0));
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 0.0)));

        CameraAngles target = planner.targetAngles(
                path,
                new NavigationPoint(0.0, 64.0, 0.0),
                PathProgress.start(),
                new CameraAngles(90.0, 12.0));

        assertEquals(new CameraAngles(90.0, 0.0), target);
    }

    @Test
    void clampsDownwardPitchSoLookAheadDoesNotAimAtFeet() {
        CameraTargetPlanner planner = new CameraTargetPlanner(new CameraTargetSettings(3.5, 0.35, 0.0));
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 61.0, 0.0),
                new NavigationPoint(0.0, 61.0, 2.0)));

        CameraAngles target = planner.targetAngles(
                path,
                new NavigationPoint(0.0, 64.0, 0.0),
                PathProgress.start(),
                new CameraAngles(0.0, 0.0));

        assertEquals(0.0, target.yawDegrees());
        assertTrue(target.pitchDegrees() > 0.0);
        assertTrue(target.pitchDegrees() <= 18.0);
    }

    @Test
    void projectsPositionOnCorridorBeforeLookingAhead() {
        CameraTargetPlanner planner = new CameraTargetPlanner(new CameraTargetSettings(3.5, 0.35, 0.0));
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 2.0),
                new NavigationPoint(0.0, 64.0, 6.0)));

        CameraAngles target = planner.targetAngles(
                path,
                new NavigationPoint(0.9, 64.0, 4.0),
                PathProgress.start(),
                new CameraAngles(180.0, 0.0));

        assertTrue(Math.abs(target.yawDegrees()) <= 45.0);
        assertEquals(0.0, target.pitchDegrees());
    }
}
