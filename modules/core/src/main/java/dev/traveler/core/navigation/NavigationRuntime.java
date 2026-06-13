package dev.traveler.core.navigation;

import dev.traveler.core.debug.PathfinderDebugState;
import java.util.Objects;
import java.util.Optional;

public final class NavigationRuntime {
    private static final double NANOS_TO_SECONDS = 1.0E-9;
    private static final double MAX_DELTA_SECONDS = 0.1;

    private final TravelerNavigationState navigationState;
    private final NavigationAgentPort agentPort;
    private final NavigationController controller;
    private final PathfinderDebugState debugState;
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
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        this.agentPort = Objects.requireNonNull(agentPort, "agentPort");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.debugState = Objects.requireNonNull(debugState, "debugState");
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
        NavigationControlFrame frame = controller.update(session.path(), input.orElseThrow(), controllerState);
        debugState.updateNavigation(input.orElseThrow(), frame);
        controllerState = frame.state();
        applyFrame(frame);
    }

    private void applyFrame(NavigationControlFrame frame) {
        if (frame.completed()) {
            navigationState.stop("navigation completed");
            releaseIfNeeded();
            return;
        }
        agentPort.apply(frame);
        released = false;
    }

    private void resetProgressWhenSessionChanges(NavigationSession session) {
        if (session == activeSession) {
            return;
        }
        activeSession = session;
        controllerState = NavigationControllerState.start();
    }

    private void releaseIfNeeded() {
        if (released) {
            return;
        }
        agentPort.release();
        debugState.clearNavigation();
        controllerState = NavigationControllerState.start();
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
