package dev.traveler.mc.v1_21_11.fabric.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.input.MovementIntent;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FabricNavigationRuntimeTest {
    @Test
    void updatesNavigationFromRenderDeltaAndAppliesIntent() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAdapter adapter = new TestAdapter(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        FabricNavigationRuntime runtime = new FabricNavigationRuntime(navigationState, adapter);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(-4.0, 64.0, 6.0))), "test");

        runtime.update(1_000_000_000L);
        runtime.update(1_016_000_000L);

        assertEquals(2, adapter.frames.size());
        assertEquals(0.016, adapter.frames.getLast().deltaSeconds());
        assertEquals(new MovementIntent(true, false, false, true, false, true), adapter.intents.getLast());
    }

    @Test
    void releasesInputWhenNavigationStops() {
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TestAdapter adapter = new TestAdapter(new NavigationPoint(0.0, 64.0, 0.0), new CameraAngles(0.0, 0.0));
        FabricNavigationRuntime runtime = new FabricNavigationRuntime(navigationState, adapter);
        navigationState.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 4.0))), "test");

        runtime.update(1_000_000_000L);
        navigationState.stop("manual");
        runtime.update(1_016_000_000L);

        assertTrue(adapter.released);
    }

    private static final class TestAdapter implements ClientNavigationAdapter {
        private final List<NavigationFrameInput> frames = new ArrayList<>();
        private final List<MovementIntent> intents = new ArrayList<>();
        private final NavigationPoint position;
        private final CameraAngles cameraAngles;
        private boolean released;

        private TestAdapter(NavigationPoint position, CameraAngles cameraAngles) {
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
            intents.add(frame.intent());
        }

        @Override
        public void release() {
            released = true;
        }
    }
}
