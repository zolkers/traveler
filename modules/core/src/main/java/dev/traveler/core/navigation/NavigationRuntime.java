package dev.traveler.core.navigation;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.diagnostics.MovementFailureReportContext;
import dev.traveler.core.navigation.diagnostics.MovementFailureReporter;
import dev.traveler.core.navigation.recovery.MovementFailure;
import dev.traveler.core.navigation.recovery.MovementFailureKind;
import dev.traveler.core.navigation.recovery.MovementProgressMonitor;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.settings.TravelerSettings;
import java.util.Objects;
import java.util.Optional;

public final class NavigationRuntime {
    private static final double NANOS_TO_SECONDS = 1.0E-9;
    private static final double MAX_DELTA_SECONDS = 0.1;

    private final TravelerNavigationState navigationState;
    private final NavigationAgentPort agentPort;
    private final NavigationController controller;
    private final PathfinderDebugState debugState;
    private final MovementProgressMonitor progressMonitor;
    private final MovementFailureReporter failureReporter;
    private NavigationControllerState controllerState = NavigationControllerState.start();
    private NavigationSession activeSession;
    private long previousNanos = -1L;
    private boolean released = true;

    public NavigationRuntime(TravelerNavigationState navigationState, NavigationAgentPort agentPort) {
        this(navigationState, agentPort, new PathfinderDebugState());
    }

    public NavigationRuntime(
            TravelerNavigationState navigationState,
            NavigationAgentPort agentPort,
            PathfinderDebugState debugState) {
        this(navigationState, agentPort, NavigationController.standard(), debugState);
    }

    public NavigationRuntime(
            TravelerNavigationState navigationState,
            NavigationAgentPort agentPort,
            PathfinderDebugState debugState,
            MovementFailureReporter failureReporter) {
        this(
                navigationState,
                agentPort,
                NavigationController.standard(),
                debugState,
                new MovementProgressMonitor(TravelerSettings.standard().movementHealthSettings()),
                failureReporter);
    }

    NavigationRuntime(
            TravelerNavigationState navigationState,
            NavigationAgentPort agentPort,
            NavigationController controller) {
        this(navigationState, agentPort, controller, new PathfinderDebugState());
    }

    NavigationRuntime(
            TravelerNavigationState navigationState,
            NavigationAgentPort agentPort,
            NavigationController controller,
            PathfinderDebugState debugState) {
        this(
                navigationState,
                agentPort,
                controller,
                debugState,
                new MovementProgressMonitor(TravelerSettings.standard().movementHealthSettings()));
    }

    NavigationRuntime(
            TravelerNavigationState navigationState,
            NavigationAgentPort agentPort,
            NavigationController controller,
            PathfinderDebugState debugState,
            MovementProgressMonitor progressMonitor) {
        this(
                navigationState,
                agentPort,
                controller,
                debugState,
                progressMonitor,
                MovementFailureReporter.noop());
    }

    NavigationRuntime(
            TravelerNavigationState navigationState,
            NavigationAgentPort agentPort,
            NavigationController controller,
            PathfinderDebugState debugState,
            MovementProgressMonitor progressMonitor,
            MovementFailureReporter failureReporter) {
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        this.agentPort = Objects.requireNonNull(agentPort, "agentPort");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.progressMonitor = Objects.requireNonNull(progressMonitor, "progressMonitor");
        this.failureReporter = Objects.requireNonNull(failureReporter, "failureReporter");
    }

    public void update(long nowNanos) {
        double deltaSeconds = deltaSeconds(nowNanos);
        Optional<NavigationSession> session = navigationState.activeSession();
        if (session.isEmpty()) {
            activeSession = null;
            releaseIfNeeded();
            return;
        }
        updateActiveSession(session.orElseThrow(), deltaSeconds);
    }

    private void updateActiveSession(NavigationSession session, double deltaSeconds) {
        resetProgressWhenSessionChanges(session);
        Optional<NavigationFrameInput> input = agentPort.frameInput(deltaSeconds);
        if (input.isEmpty()) {
            releaseIfNeeded();
            return;
        }
        NavigationFrameInput frameInput = input.orElseThrow();
        NavigationControlFrame frame = controller.update(session.path(), frameInput, controllerState);
        debugState.updateNavigation(frameInput, frame);
        controllerState = frame.state();
        if (movementFailure(session, frameInput, frame).isPresent()) {
            return;
        }
        requestLookaheadReplanIfNeeded(session, frameInput, frame);
        applyFrame(session, frame);
    }

    private Optional<MovementFailure> movementFailure(
            NavigationSession session,
            NavigationFrameInput input,
            NavigationControlFrame frame) {
        if (frame.completed()) {
            return Optional.empty();
        }
        Optional<MovementFailure> failure = progressMonitor.update(session.path(), input, frame);
        failure.ifPresent(value -> handleMovementFailure(session, input, frame, value));
        return failure;
    }

    private void handleMovementFailure(
            NavigationSession session,
            NavigationFrameInput input,
            NavigationControlFrame frame,
            MovementFailure failure) {
        reportMovementFailure(session, input, frame, failure);
        String message = "navigation recovery requested reason=" + failure.kind();
        if (failure.kind() == MovementFailureKind.PATH_DIVERGENCE) {
            session.goalPlan().ifPresentOrElse(
                    goalPlan -> navigationState.requestSegmentRepair(goalPlan, message, input.position()),
                    () -> navigationState.stop("navigation stopped reason=" + failure.kind()));
            progressMonitor.reset();
            return;
        }
        session.goalPlan().ifPresentOrElse(
                goalPlan -> navigationState.requestReplan(goalPlan, message),
                () -> navigationState.stop("navigation stopped reason=" + failure.kind()));
        releaseIfNeeded();
    }

    private void reportMovementFailure(
            NavigationSession session,
            NavigationFrameInput input,
            NavigationControlFrame frame,
            MovementFailure failure) {
        try {
            failureReporter.report(new MovementFailureReportContext(session, input, frame, failure));
        } catch (RuntimeException ignored) {
            // Diagnostics must never prevent control release or recovery.
        }
    }

    private void applyFrame(NavigationSession session, NavigationControlFrame frame) {
        if (frame.completed()) {
            if (session.goalPlan().filter(NavigationGoalPlan::needsReplanAfterCompletion).isPresent()) {
                if (!navigationState.activatePreparedLookahead()) {
                    navigationState.requestLookaheadReplan(
                            session.goalPlan().orElseThrow(),
                            "navigation segment completed; replan requested",
                            replanStart(session));
                }
            } else {
                navigationState.stop("navigation completed");
                releaseIfNeeded();
            }
            return;
        }
        agentPort.apply(frame);
        released = false;
    }

    private void requestLookaheadReplanIfNeeded(
            NavigationSession session,
            NavigationFrameInput input,
            NavigationControlFrame frame) {
        if (frame.completed() || navigationState.hasPendingReplanRequest()) {
            return;
        }
        session.goalPlan()
                .filter(goalPlan -> goalPlan.needsLookaheadReplan(distanceToSegmentEnd(session, input)))
                .ifPresent(goalPlan -> navigationState.requestLookaheadReplan(
                        goalPlan,
                        "navigation segment near frontier; lookahead replan requested",
                        replanStart(session)));
    }

    private static double distanceToSegmentEnd(NavigationSession session, NavigationFrameInput input) {
        return input.position().horizontalDistanceTo(session.path().lastNode());
    }

    private static WorldPoint replanStart(NavigationSession session) {
        return session.path().lastNode();
    }

    private void resetProgressWhenSessionChanges(NavigationSession session) {
        if (session == activeSession) {
            return;
        }
        activeSession = session;
        controllerState = NavigationControllerState.start();
        progressMonitor.reset();
    }

    private void releaseIfNeeded() {
        if (released) {
            return;
        }
        agentPort.release();
        debugState.clearNavigation();
        controllerState = NavigationControllerState.start();
        progressMonitor.reset();
        released = true;
    }

    private double deltaSeconds(long nowNanos) {
        if (previousNanos < 0L) {
            previousNanos = nowNanos;
            return 0.0;
        }
        long elapsed = Math.max(0L, nowNanos - previousNanos);
        previousNanos = nowNanos;
        return Math.min(elapsed * NANOS_TO_SECONDS, MAX_DELTA_SECONDS);
    }
}
