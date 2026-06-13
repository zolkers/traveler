package dev.traveler.core.navigation.steering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathSteeringControllerTest {
    @Test
    void predictsPositionAndTargetsDistanceAheadLikeGdxAiFollowPath() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.5, 0.35, 1.0, 0.75));
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 8.0)));
        AgentMotionState motion = new AgentMotionState(
                true,
                false,
                new HorizontalVector(0.0, 2.0),
                0.0);

        SteeringPlan plan = controller.plan(path, new NavigationPoint(1.0, 64.0, 2.0), motion, 1);

        assertEquals(5.0, plan.distanceOnPath());
        assertEquals(new NavigationPoint(0.0, 64.0, 5.0), plan.pathTarget());
        assertTrue(plan.steeringTarget().x() < 0.0);
        assertEquals(new HorizontalVector(0.0, 1.0), plan.tangent());
    }

    @Test
    void keepsTargetOnCenterlineWhenAgentIsInsideCorridor() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.0, 0.35, 1.0, 0.75));
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 8.0)));

        SteeringPlan plan = controller.plan(
                path,
                new NavigationPoint(0.1, 64.0, 2.0),
                AgentMotionState.groundedStill(),
                1);

        assertEquals(new NavigationPoint(0.0, 64.0, 4.0), plan.steeringTarget());
        assertEquals(0.1, plan.lateralError());
    }

    @Test
    void steersBackTowardFinalNodeAfterOvershoot() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.0, 0.35, 1.0, 0.75));
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 4.0)));
        NavigationPoint position = new NavigationPoint(0.0, 64.0, 6.0);

        SteeringPlan plan = controller.plan(path, position, AgentMotionState.groundedStill(), 1);

        assertTrue(plan.steeringTarget().z() < position.z());
        assertTrue(plan.desiredVectorFrom(position).z() < 0.0);
    }
}
