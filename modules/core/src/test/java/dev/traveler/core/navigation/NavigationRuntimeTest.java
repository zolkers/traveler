package dev.traveler.core.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.recovery.MovementHealthSettings;
import dev.traveler.core.navigation.recovery.MovementProgressMonitor;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteGoal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NavigationRuntimeTest {
    @Test
    void updatesNavigationFromFrameDeltaAndAppliesIntentThroughPort() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(navigationState, agent);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(-4.0, 64.0, 6.0))), "test");

        runtime.update(1_000_000_000L);
        runtime.update(1_016_000_000L);

        assertEquals(2, agent.frames.size());
        assertEquals(0.016, agent.frames.getLast().deltaSeconds());
        assertEquals(new MovementIntent(true, false, false, true, false, true), agent.intents.getLast());
    }

    @Test
    void releasesInputWhenNavigationStops() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(navigationState, agent);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 4.0))), "test");

        runtime.update(1_000_000_000L);
        navigationState.stop("manual");
        runtime.update(1_016_000_000L);

        assertTrue(agent.released);
    }

    @Test
    void resetsPathProgressWhenActiveSessionChanges() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(navigationState, agent);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.1, 64.0, 0.0),
                new NavigationPoint(8.0, 64.0, 0.0))), "first");

        runtime.update(1_000_000_000L);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(4.0, 64.0, 0.0),
                new NavigationPoint(8.0, 64.0, 0.0))), "second");
        runtime.update(1_016_000_000L);

        assertEquals(new PathProgress(1), agent.controlFrames.getLast().state().progress());
    }

    @Test
    void storesLatestNavigationDebugFrameWhileActive() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        PathfinderDebugState debugState = new PathfinderDebugState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(navigationState, agent, debugState);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 4.0))), "test");

        runtime.update(1_000_000_000L);

        assertTrue(debugState.latestNavigation().isPresent());
        assertTrue(debugState.navigationSummary().orElseThrow().contains("phase=APPROACH"));
    }

    @Test
    void clearsNavigationDebugWhenRuntimeReleasesControls() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        PathfinderDebugState debugState = new PathfinderDebugState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(navigationState, agent, debugState);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 4.0))), "test");

        runtime.update(1_000_000_000L);
        navigationState.stop("manual");
        runtime.update(1_016_000_000L);

        assertFalse(debugState.latestNavigation().isPresent());
    }

    @Test
    void requestsReplanWhenMovementIsCommandedButPositionDoesNotProgress() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(
                navigationState,
                agent,
                NavigationController.standard(),
                new PathfinderDebugState(),
                new MovementProgressMonitor(new MovementHealthSettings(0.05, 0.15, 0.0)));
        NavigationGoalPlan goalPlan = new NavigationGoalPlan(
                RouteGoal.xz(0, 4),
                RouteGoal.xz(0, 4),
                true);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 4.0))), "test", goalPlan);

        runtime.update(1_000_000_000L);
        runtime.update(1_100_000_000L);
        runtime.update(1_200_000_000L);

        assertTrue(navigationState.pendingReplanRequest().isPresent());
        assertEquals(RouteGoal.xz(0, 4), navigationState.pendingReplanRequest().orElseThrow().goal());
        assertTrue(agent.released);
    }

    @Test
    void requestsLookaheadReplanBeforeSegmentCompletionWithoutStoppingCurrentSession() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(14.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(navigationState, agent);
        NavigationGoalPlan goalPlan = new NavigationGoalPlan(
                RouteGoal.xz(100, 0),
                RouteGoal.xz(20, 0),
                false,
                8.0);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(20.0, 64.0, 0.0))), "test", goalPlan);

        runtime.update(1_000_000_000L);

        NavigationReplanRequest request = navigationState.pendingReplanRequest().orElseThrow();
        assertEquals(RouteGoal.xz(100, 0), request.goal());
        assertTrue(request.preserveActiveSession());
        assertTrue(navigationState.activeSession().isPresent());
        assertFalse(agent.released);
        assertFalse(agent.intents.isEmpty());
    }

    @Test
    void activatesPreparedLookaheadAtSegmentCompletionInsteadOfRequestingFreshReplan() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAgentPort agent = new TestAgentPort(new NavigationPoint(20.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        NavigationRuntime runtime = new NavigationRuntime(navigationState, agent);
        NavigationGoalPlan currentPlan = new NavigationGoalPlan(
                RouteGoal.xz(100, 0),
                RouteGoal.xz(20, 0),
                false,
                8.0);
        NavigationGoalPlan preparedPlan = new NavigationGoalPlan(
                RouteGoal.xz(100, 0),
                RouteGoal.xz(40, 0),
                false,
                8.0);
        NavigationPath preparedPath = NavigationPath.of(List.of(
                new NavigationPoint(20.0, 64.0, 0.0),
                new NavigationPoint(40.0, 64.0, 0.0)));
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(20.0, 64.0, 0.0))), "current", currentPlan);
        navigationState.prepareLookahead(preparedPath, "prepared", preparedPlan);

        runtime.update(1_000_000_000L);

        assertEquals(preparedPath, navigationState.activeSession().orElseThrow().path());
        assertTrue(navigationState.pendingReplanRequest().isEmpty());
    }

    private static final class TestAgentPort implements NavigationAgentPort {
        private final List<NavigationFrameInput> frames = new ArrayList<>();
        private final List<NavigationControlFrame> controlFrames = new ArrayList<>();
        private final List<MovementIntent> intents = new ArrayList<>();
        private final NavigationPoint position;
        private final CameraAngles cameraAngles;
        private boolean released;

        private TestAgentPort(NavigationPoint position, CameraAngles cameraAngles) {
            this.position = position;
            this.cameraAngles = cameraAngles;
        }

        @Override
        public Optional<NavigationFrameInput> frameInput(double deltaSeconds) {
            NavigationFrameInput input = new NavigationFrameInput(position, cameraAngles, deltaSeconds);
            frames.add(input);
            return Optional.of(input);
        }

        @Override
        public void apply(NavigationControlFrame frame) {
            controlFrames.add(frame);
            intents.add(frame.intent());
        }

        @Override
        public void release() {
            released = true;
        }
    }
}
