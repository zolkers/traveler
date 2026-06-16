package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.api.RecoveryAction;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.internal.RecoveryPlanner;
import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;
import java.util.Optional;

public final class MovementProgressMonitor {
    private final MovementHealthSettings settings;
    private final MovementHealthProbe probe;
    private final MovementHealthPolicyRegistry policies;
    private final RecoveryPlanner recoveryPlanner = new RecoveryPlanner();
    private WorldPoint lastProgressPosition;
    private double positionStagnantSeconds;
    private int positionStagnantTicks;
    private MovementHealthState state = MovementHealthState.empty();

    public MovementProgressMonitor(MovementHealthSettings settings) {
        this(settings, new RouteMovementHealthProbe(), MovementHealthPolicyRegistry.standard());
    }

    public MovementProgressMonitor(
            MovementHealthSettings settings,
            MovementHealthProbe probe,
            MovementHealthPolicyRegistry policies) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.probe = Objects.requireNonNull(probe, "probe");
        this.policies = Objects.requireNonNull(policies, "policies");
    }

    public Optional<MovementFailure> update(NavigationFrameInput input, MovementIntent intent) {
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        MovementIntent movementIntent = Objects.requireNonNull(intent, "intent");
        if (!movementIntent.moving()) {
            resetPositionWatchdog();
            return Optional.empty();
        }
        if (lastProgressPosition == null) {
            lastProgressPosition = frameInput.position();
            positionStagnantSeconds = frameInput.deltaSeconds();
            positionStagnantTicks = 1;
            return failureIfPositionStuck();
        }
        if (lastProgressPosition.distanceTo(frameInput.position()) >= settings.minimumProgressDistance()) {
            lastProgressPosition = frameInput.position();
            positionStagnantSeconds = 0.0;
            positionStagnantTicks = 0;
            return Optional.empty();
        }
        positionStagnantSeconds += frameInput.deltaSeconds();
        positionStagnantTicks++;
        return failureIfPositionStuck();
    }

    public Optional<RecoveryAction> updateRecoveryAction(NavigationFrameInput input, MovementIntent intent) {
        return update(input, intent).map(recoveryPlanner::plan);
    }

    public Optional<MovementFailure> update(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControlFrame frame) {
        Objects.requireNonNull(path, "path");
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        NavigationControlFrame controlFrame = Objects.requireNonNull(frame, "frame");
        if (!controlFrame.intent().moving()) {
            reset();
            return Optional.empty();
        }
        MovementHealthSnapshot snapshot = probe.sample(path, frameInput, controlFrame);
        if (!state.tracksSameSegment(snapshot)) {
            state = state.resetFor(snapshot);
        }
        MovementHealthEvaluation evaluation = policies.policyFor(snapshot.action()).evaluate(snapshot, settings);
        return update(snapshot, evaluation, frameInput.deltaSeconds());
    }

    public Optional<RecoveryAction> updateRecoveryAction(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControlFrame frame) {
        return update(path, input, frame).map(recoveryPlanner::plan);
    }

    public void reset() {
        lastProgressPosition = null;
        positionStagnantSeconds = 0.0;
        positionStagnantTicks = 0;
        state = MovementHealthState.empty();
    }

    private Optional<MovementFailure> update(
            MovementHealthSnapshot snapshot,
            MovementHealthEvaluation evaluation,
            double deltaSeconds) {
        switch (evaluation.phase()) {
            case IDLE -> {
                state = state.resetFor(snapshot);
                return Optional.empty();
            }
            case SETUP -> {
                double setupSeconds = state.setupSeconds() + deltaSeconds;
                double divergentSeconds = evaluation.lateralDistance() <= evaluation.allowedLateralDistance()
                        ? 0.0
                        : state.divergentSeconds() + deltaSeconds;
                int setupTicks = state.setupTicks() + 1;
                int divergentTicks = evaluation.lateralDistance() <= evaluation.allowedLateralDistance()
                        ? 0
                        : state.divergentTicks() + 1;
                state = new MovementHealthState(
                        snapshot.nextNodeIndex(),
                        snapshot.action(),
                        null,
                        null,
                        0.0,
                        divergentSeconds,
                        setupSeconds,
                        0,
                        divergentTicks,
                        setupTicks);
                if (timedOut(
                        divergentSeconds,
                        divergentTicks,
                        settings.pathDivergenceAfterSeconds(),
                        settings.pathDivergenceAfterTicks())) {
                    return Optional.of(new MovementFailure(
                            MovementFailureKind.PATH_DIVERGENCE,
                            "navigation drifted outside the path corridor"));
                }
                if (!timedOut(
                        setupSeconds,
                        setupTicks,
                        settings.actionSetupTimeoutSeconds(),
                        settings.actionSetupTimeoutTicks())) {
                    return Optional.empty();
                }
                return Optional.of(new MovementFailure(
                        MovementFailureKind.ACTION_SETUP_TIMEOUT,
                        "navigation action setup exceeded timeout"));
            }
            case COMMITTED -> {
                state = state.resetFor(snapshot);
                return Optional.empty();
            }
            case ACTIVE -> {
                return updateActive(snapshot, evaluation, deltaSeconds);
            }
        }
        throw new IllegalStateException("Unhandled movement health phase " + evaluation.phase());
    }

    private Optional<MovementFailure> updateActive(
            MovementHealthSnapshot snapshot,
            MovementHealthEvaluation evaluation,
            double deltaSeconds) {
        double divergentSeconds = evaluation.lateralDistance() <= evaluation.allowedLateralDistance()
                ? 0.0
                : state.divergentSeconds() + deltaSeconds;
        int divergentTicks = evaluation.lateralDistance() <= evaluation.allowedLateralDistance()
                ? 0
                : state.divergentTicks() + 1;
        if (timedOut(
                divergentSeconds,
                divergentTicks,
                settings.pathDivergenceAfterSeconds(),
                settings.pathDivergenceAfterTicks())) {
            state = new MovementHealthState(
                    snapshot.nextNodeIndex(),
                    snapshot.action(),
                    state.bestProgressValue(),
                    state.bestTargetDistance(),
                    state.stagnantSeconds(),
                    divergentSeconds,
                    0.0,
                    state.stagnantTicks(),
                    divergentTicks,
                    0);
            return Optional.of(new MovementFailure(
                    MovementFailureKind.PATH_DIVERGENCE,
                    "navigation drifted outside the path corridor"));
        }
        if (state.bestProgressValue() == null || state.bestTargetDistance() == null) {
            state = new MovementHealthState(
                    snapshot.nextNodeIndex(),
                    snapshot.action(),
                    evaluation.progressValue(),
                    evaluation.targetDistance(),
                    0.0,
                    divergentSeconds,
                    0.0,
                    0,
                    divergentTicks,
                    0);
            return Optional.empty();
        }
        boolean advanced = advanced(evaluation, state);
        if (advanced) {
            state = new MovementHealthState(
                    snapshot.nextNodeIndex(),
                    snapshot.action(),
                    evaluation.progressValue(),
                    evaluation.targetDistance(),
                    0.0,
                    divergentSeconds,
                    0.0,
                    0,
                    divergentTicks,
                    0);
            return Optional.empty();
        }
        double stagnantSeconds = state.stagnantSeconds() + deltaSeconds;
        int stagnantTicks = state.stagnantTicks() + 1;
        state = new MovementHealthState(
                snapshot.nextNodeIndex(),
                snapshot.action(),
                state.bestProgressValue(),
                state.bestTargetDistance(),
                stagnantSeconds,
                divergentSeconds,
                0.0,
                stagnantTicks,
                divergentTicks,
                0);
        if (!timedOut(stagnantSeconds, stagnantTicks, settings.stuckAfterSeconds(), settings.stuckAfterTicks())) {
            return Optional.empty();
        }
        return Optional.of(new MovementFailure(
                MovementFailureKind.STUCK_NO_PROGRESS,
                "movement commanded but segment did not make progress"));
    }

    private boolean advanced(MovementHealthEvaluation evaluation, MovementHealthState currentState) {
        return evaluation.progressValue() - currentState.bestProgressValue()
                        >= settings.minimumProgressDistance()
                || currentState.bestTargetDistance() - evaluation.targetDistance()
                        >= settings.minimumProgressDistance();
    }

    private void resetPositionWatchdog() {
        lastProgressPosition = null;
        positionStagnantSeconds = 0.0;
        positionStagnantTicks = 0;
    }

    private Optional<MovementFailure> failureIfPositionStuck() {
        if (!timedOut(
                positionStagnantSeconds,
                positionStagnantTicks,
                settings.stuckAfterSeconds(),
                settings.stuckAfterTicks())) {
            return Optional.empty();
        }
        return Optional.of(new MovementFailure(
                MovementFailureKind.STUCK_NO_PROGRESS,
                "movement commanded but position did not progress"));
    }

    private static boolean timedOut(double seconds, int ticks, double secondsLimit, int tickLimit) {
        return seconds >= secondsLimit || ticks >= tickLimit;
    }
}
