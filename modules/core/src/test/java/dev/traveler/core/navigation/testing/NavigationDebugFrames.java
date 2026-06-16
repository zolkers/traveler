package dev.traveler.core.navigation.testing;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.plan.ActionIntent;
import dev.traveler.core.navigation.plan.MovementVectorIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;

public final class NavigationDebugFrames {
    private NavigationDebugFrames() {}

    public static NavigationControlFrame approachFrame(WorldPoint targetPoint) {
        MovementIntent intent = new MovementIntent(true, false, false, false, false, true);
        MovementTarget target = MovementTarget.follow(targetPoint);
        return new NavigationControlFrame(
                new NavigationControllerState(PathProgress.start(), intent, LocomotionExecutionState.start()),
                intent,
                new CameraAngles(0.0, 0.0),
                target,
                approachPlan(target),
                false);
    }

    private static NavigationFramePlan approachPlan(MovementTarget target) {
        return new NavigationFramePlan(
                NavigationPhase.APPROACH,
                PathProgress.start(),
                target,
                new MovementVectorIntent(new HorizontalVector(0.0, 1.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(0.0, 0.0),
                ActionIntent.none(),
                new SpeedIntent(1.0, true),
                LocomotionExecutionState.start(),
                false);
    }
}
