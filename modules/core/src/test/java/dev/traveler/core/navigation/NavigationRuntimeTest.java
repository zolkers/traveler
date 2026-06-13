package dev.traveler.core.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.spatial.NavigationPoint;
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
