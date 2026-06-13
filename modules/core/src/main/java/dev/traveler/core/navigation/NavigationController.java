package dev.traveler.core.navigation;

import dev.traveler.core.navigation.camera.CameraAimController;
import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathFollowController;
import dev.traveler.core.navigation.follow.PathFollowFrame;
import dev.traveler.core.navigation.follow.PathFollowSettings;
import dev.traveler.core.navigation.input.MovementInputPlanner;
import dev.traveler.core.navigation.input.MovementInputSettings;
import dev.traveler.core.navigation.input.MovementIntent;
import java.util.Objects;

public final class NavigationController {
    private final PathFollowController pathFollowController;
    private final CameraAimController cameraAimController;
    private final MovementInputPlanner inputPlanner;

    public NavigationController(
            PathFollowController pathFollowController,
            CameraAimController cameraAimController,
            MovementInputPlanner inputPlanner) {
        this.pathFollowController = Objects.requireNonNull(pathFollowController, "pathFollowController");
        this.cameraAimController = Objects.requireNonNull(cameraAimController, "cameraAimController");
        this.inputPlanner = Objects.requireNonNull(inputPlanner, "inputPlanner");
    }

    public static NavigationController standard() {
        return standard(CameraAimSettings.standard());
    }

    public static NavigationController standard(CameraAimSettings cameraAimSettings) {
        return new NavigationController(
                new PathFollowController(PathFollowSettings.standard()),
                new CameraAimController(cameraAimSettings),
                new MovementInputPlanner(MovementInputSettings.standard()));
    }

    public NavigationControlFrame update(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControllerState state) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        NavigationControllerState currentState = Objects.requireNonNull(state, "state");
        PathFollowFrame follow = pathFollowController.update(
                navigationPath,
                frameInput.position(),
                currentState.progress(),
                frameInput.motionState());
        if (follow.completed()) {
            return completedFrame(frameInput, follow);
        }
        CameraAngles cameraAngles = cameraAimController.update(
                frameInput.cameraAngles(),
                CameraAimController.targetAngles(frameInput.position(), follow.steeringTarget()),
                frameInput.deltaSeconds());
        MovementIntent intent = movementIntent(frameInput, currentState, follow);
        NavigationControllerState nextState = new NavigationControllerState(follow.progress(), intent);
        return new NavigationControlFrame(nextState, intent, cameraAngles, follow.movementTarget(), false);
    }

    private NavigationControlFrame completedFrame(NavigationFrameInput input, PathFollowFrame follow) {
        NavigationControllerState nextState = new NavigationControllerState(follow.progress(), MovementIntent.idle());
        return new NavigationControlFrame(
                nextState, MovementIntent.idle(), input.cameraAngles(), follow.movementTarget(), true);
    }

    private MovementIntent movementIntent(
            NavigationFrameInput input,
            NavigationControllerState state,
            PathFollowFrame follow) {
        MovementIntent intent = inputPlanner.plan(
                input.position(),
                follow.steeringPlan(),
                input.cameraAngles().yawDegrees(),
                state.previousIntent(),
                follow.locomotionPlan(),
                input.motionState());
        if (follow.speedScale() >= 0.5) {
            return intent;
        }
        return intent.withSprint(false);
    }
}
