package dev.traveler.core.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerNavigationStateTest {
    @Test
    void storesDefensiveNavigationSessionSnapshot() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(1.0, 64.0, 0.0)));

        state.start(path, "testing");

        NavigationSession session = state.activeSession().orElseThrow();
        assertEquals(path, session.path());
        assertEquals("testing", session.message());
    }

    @Test
    void stopClearsActiveSession() {
        TravelerNavigationState state = new TravelerNavigationState();
        state.start(NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(1.0, 64.0, 0.0))), "testing");

        state.stop("done");

        assertTrue(state.activeSession().isEmpty());
        assertEquals("done", state.latestMessage().orElseThrow());
    }
}
