package dev.traveler.core.navigation;

import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.control.ControlProjectionFrame;
import dev.traveler.core.navigation.control.ControlProjector;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.NavigationFramePlanner;
import java.util.Objects;

public final class NavigationController {
    private final NavigationFramePlanner planner;
    private final ControlProjector projector;

    public NavigationController(
            NavigationFramePlanner planner,
            ControlProjector projector) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.projector = Objects.requireNonNull(projector, "projector");
    }

    public static NavigationController standard() {
        return standard(CameraAimSettings.standard());
    }

    public static NavigationController standard(CameraAimSettings cameraAimSettings) {
        return new NavigationController(NavigationFramePlanner.standard(), ControlProjector.standard(cameraAimSettings));
    }

    public NavigationControlFrame update(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControllerState state) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        NavigationControllerState currentState = Objects.requireNonNull(state, "state");
        NavigationFramePlan plan = planner.plan(navigationPath, frameInput, currentState);
        ControlProjectionFrame projection = projector.project(plan, frameInput, currentState.previousIntent());
        NavigationControllerState nextState = new NavigationControllerState(
                plan.routeProgress(),
                projection.intent(),
                plan.locomotionState());
        return new NavigationControlFrame(
                nextState,
                projection.intent(),
                projection.cameraAngles(),
                plan.movementTarget(),
                plan,
                plan.completed());
    }
}
