package dev.traveler.mc.v1_21_11.fabric.navigation;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationController;
import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.TravelerNavigationState;
import java.util.Objects;
import java.util.Optional;

public final class FabricNavigationRuntime {
    private static final double NANOS_TO_SECONDS = 1.0E-9;
    private static final double MAX_DELTA_SECONDS = 0.1;

    private final TravelerNavigationState navigationState;
    private final ClientNavigationAdapter adapter;
    private final NavigationController controller;
    private NavigationControllerState controllerState = NavigationControllerState.start();
    private NavigationSession activeSession;
    private long previousNanos = -1L;
    private boolean released = true;

    public FabricNavigationRuntime(TravelerNavigationState navigationState, ClientNavigationAdapter adapter) {
        this(navigationState, adapter, NavigationController.standard());
    }

    FabricNavigationRuntime(
            TravelerNavigationState navigationState,
            ClientNavigationAdapter adapter,
            NavigationController controller) {
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.controller = Objects.requireNonNull(controller, "controller");
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
        Optional<NavigationFrameInput> input = adapter.frameInput(deltaSeconds);
        if (input.isEmpty()) {
            releaseIfNeeded();
            return;
        }
        NavigationControlFrame frame = controller.update(
                session.path(),
                input.orElseThrow(),
                controllerState);
        controllerState = frame.state();
        applyFrame(frame);
    }

    private void applyFrame(NavigationControlFrame frame) {
        if (frame.completed()) {
            navigationState.stop("navigation completed");
            releaseIfNeeded();
            return;
        }
        adapter.apply(frame);
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
        adapter.release();
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
