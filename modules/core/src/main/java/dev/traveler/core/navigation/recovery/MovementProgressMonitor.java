package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;
import java.util.Optional;

public final class MovementProgressMonitor {
    private final MovementHealthSettings settings;
    private final MovementProgressMetric metric;
    private NavigationPoint lastProgressPosition;
    private Double lastRouteProgress;
    private MovementAction lastAction;
    private double stagnantSeconds;
    private double divergentSeconds;
    private double actionSetupSeconds;

    public MovementProgressMonitor(MovementHealthSettings settings) {
        this(settings, new RouteActionProgressMetric());
    }

    public MovementProgressMonitor(MovementHealthSettings settings, MovementProgressMetric metric) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.metric = Objects.requireNonNull(metric, "metric");
    }

    public Optional<MovementFailure> update(NavigationFrameInput input, MovementIntent intent) {
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        MovementIntent movementIntent = Objects.requireNonNull(intent, "intent");
        if (!movementIntent.moving()) {
            reset();
            return Optional.empty();
        }
        if (lastProgressPosition == null) {
            lastProgressPosition = frameInput.position();
            stagnantSeconds = frameInput.deltaSeconds();
            return failureIfStuck();
        }
        if (lastProgressPosition.distanceTo(frameInput.position()) >= settings.minimumProgressDistance()) {
            lastProgressPosition = frameInput.position();
            stagnantSeconds = 0.0;
            return Optional.empty();
        }
        stagnantSeconds += frameInput.deltaSeconds();
        return failureIfStuck();
    }

    public Optional<MovementFailure> update(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControlFrame frame) {
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        NavigationControlFrame controlFrame = Objects.requireNonNull(frame, "frame");
        if (!controlFrame.intent().moving()) {
            reset();
            return Optional.empty();
        }
        ProgressSample sample = metric.sample(
                Objects.requireNonNull(path, "path"),
                frameInput,
                controlFrame);
        resetRouteProgressWhenActionChanges(sample.action());
        Optional<MovementFailure> setupFailure = updateActionSetup(sample, frameInput.deltaSeconds());
        if (setupFailure.isPresent()) {
            return setupFailure;
        }
        if (sample.phase() == NavigationPhase.RECOVER) {
            resetRouteHealth();
            return Optional.empty();
        }
        Optional<MovementFailure> divergenceFailure = updatePathDivergence(sample, frameInput.deltaSeconds());
        if (divergenceFailure.isPresent()) {
            return divergenceFailure;
        }
        if (sample.phase() == NavigationPhase.ALIGN) {
            resetStuckHealth();
            return Optional.empty();
        }
        if (lastRouteProgress == null) {
            lastRouteProgress = sample.routeProgress();
            stagnantSeconds = frameInput.deltaSeconds();
            return failureIfRouteStuck(sample.action());
        }
        if (sample.routeProgress() - lastRouteProgress >= settings.minimumProgressDistance()) {
            lastRouteProgress = sample.routeProgress();
            stagnantSeconds = 0.0;
            return Optional.empty();
        }
        stagnantSeconds += frameInput.deltaSeconds();
        return failureIfRouteStuck(sample.action());
    }

    public void reset() {
        lastProgressPosition = null;
        lastRouteProgress = null;
        lastAction = null;
        stagnantSeconds = 0.0;
        divergentSeconds = 0.0;
        actionSetupSeconds = 0.0;
    }

    private void resetRouteHealth() {
        lastRouteProgress = null;
        stagnantSeconds = 0.0;
        divergentSeconds = 0.0;
    }

    private void resetStuckHealth() {
        lastRouteProgress = null;
        stagnantSeconds = 0.0;
    }

    private Optional<MovementFailure> failureIfStuck() {
        if (stagnantSeconds < settings.stuckAfterSeconds()) {
            return Optional.empty();
        }
        return Optional.of(new MovementFailure(
                MovementFailureKind.STUCK_NO_PROGRESS,
                "movement commanded but position did not progress"));
    }

    private void resetRouteProgressWhenActionChanges(MovementAction action) {
        if (action == lastAction) {
            return;
        }
        lastAction = action;
        lastRouteProgress = null;
        stagnantSeconds = 0.0;
        divergentSeconds = 0.0;
        actionSetupSeconds = 0.0;
    }

    private Optional<MovementFailure> updateActionSetup(ProgressSample sample, double deltaSeconds) {
        if (sample.phase() != NavigationPhase.ALIGN) {
            actionSetupSeconds = 0.0;
            return Optional.empty();
        }
        actionSetupSeconds += deltaSeconds;
        if (actionSetupSeconds < settings.actionSetupTimeoutSeconds()) {
            return Optional.empty();
        }
        return Optional.of(new MovementFailure(
                MovementFailureKind.ACTION_SETUP_TIMEOUT,
                "navigation action setup exceeded timeout"));
    }

    private Optional<MovementFailure> updatePathDivergence(ProgressSample sample, double deltaSeconds) {
        if (sample.lateralDistance() <= allowedLateralDistance(sample.action())) {
            divergentSeconds = 0.0;
            return Optional.empty();
        }
        divergentSeconds += deltaSeconds;
        if (divergentSeconds < settings.pathDivergenceAfterSeconds()) {
            return Optional.empty();
        }
        return Optional.of(new MovementFailure(
                MovementFailureKind.PATH_DIVERGENCE,
                "navigation drifted outside the path corridor"));
    }

    private double allowedLateralDistance(MovementAction action) {
        if (action == MovementAction.SWIM) {
            return settings.pathDivergenceDistance() * 1.5;
        }
        if (action == MovementAction.CLIMB) {
            return settings.pathDivergenceDistance() * 0.75;
        }
        return settings.pathDivergenceDistance();
    }

    private Optional<MovementFailure> failureIfRouteStuck(MovementAction action) {
        if (action == MovementAction.JUMP && stagnantSeconds <= settings.jumpGraceSeconds()) {
            return Optional.empty();
        }
        return failureIfStuck();
    }
}
