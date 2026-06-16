package dev.traveler.core.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.route.RouteGoal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerNavigationStateTest {
    @Test
    void storesDefensiveNavigationSessionSnapshot() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(1.0, 64.0, 0.0)));

        state.start(path, "testing");

        NavigationSession session = state.activeSession().orElseThrow();
        assertEquals(path, session.path());
        assertEquals("testing", session.message());
    }

    @Test
    void stopClearsActiveSession() {
        TravelerNavigationState state = new TravelerNavigationState();
        state.start(NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(1.0, 64.0, 0.0))), "testing");

        state.stop("done");

        assertTrue(state.activeSession().isEmpty());
        assertEquals("done", state.latestMessage().orElseThrow());
    }

    @Test
    void preparesLookaheadSessionWithoutReplacingActiveSession() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationPath activePath = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(4.0, 64.0, 0.0)));
        NavigationPath lookaheadPath = NavigationPath.of(List.of(
                new WorldPoint(4.0, 64.0, 0.0),
                new WorldPoint(8.0, 64.0, 0.0)));

        state.start(activePath, "active", goalPlan(100, 0, 4, 0));
        state.prepareLookahead(lookaheadPath, "lookahead", goalPlan(100, 0, 8, 0));

        assertEquals(activePath, state.activeSession().orElseThrow().path());
        assertEquals(lookaheadPath, state.preparedLookaheadSession().orElseThrow().path());
    }

    @Test
    void activatesPreparedLookaheadWhenCurrentSegmentCompletes() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationPath activePath = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(4.0, 64.0, 0.0)));
        NavigationPath lookaheadPath = NavigationPath.of(List.of(
                new WorldPoint(4.0, 64.0, 0.0),
                new WorldPoint(8.0, 64.0, 0.0)));

        state.start(activePath, "active", goalPlan(100, 0, 4, 0));
        state.prepareLookahead(lookaheadPath, "lookahead", goalPlan(100, 0, 8, 0));

        assertTrue(state.activatePreparedLookahead());
        assertEquals(lookaheadPath, state.activeSession().orElseThrow().path());
        assertTrue(state.preparedLookaheadSession().isEmpty());
        assertFalse(state.hasPendingReplanRequest());
    }

    @Test
    void preparingLookaheadClearsRedundantLookaheadRequest() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationGoalPlan activePlan = goalPlan(100, 0, 4, 0);
        NavigationPath activePath = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(4.0, 64.0, 0.0)));
        NavigationPath lookaheadPath = NavigationPath.of(List.of(
                new WorldPoint(4.0, 64.0, 0.0),
                new WorldPoint(8.0, 64.0, 0.0)));

        state.start(activePath, "active", activePlan);
        state.requestLookaheadReplan(activePlan, "redundant");
        state.prepareLookahead(lookaheadPath, "lookahead", goalPlan(100, 0, 8, 0));

        assertFalse(state.hasPendingReplanRequest());
    }

    @Test
    void replacingActiveSessionClearsPreparedLookahead() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationGoalPlan activePlan = goalPlan(100, 0, 4, 0);
        NavigationPath activePath = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(4.0, 64.0, 0.0)));
        NavigationPath lookaheadPath = NavigationPath.of(List.of(
                new WorldPoint(4.0, 64.0, 0.0),
                new WorldPoint(8.0, 64.0, 0.0)));
        NavigationPath repairPath = NavigationPath.of(List.of(
                new WorldPoint(1.0, 64.0, 1.0),
                new WorldPoint(4.0, 64.0, 0.0)));

        state.start(activePath, "active", activePlan);
        state.prepareLookahead(lookaheadPath, "lookahead", goalPlan(100, 0, 8, 0));
        state.replaceActiveSession(repairPath, "repair", activePlan);

        assertEquals(repairPath, state.activeSession().orElseThrow().path());
        assertTrue(state.preparedLookaheadSession().isEmpty());
        assertFalse(state.hasPendingReplanRequest());
        assertEquals("repair", state.latestMessage().orElseThrow());
    }

    private static NavigationGoalPlan goalPlan(int requestedX, int requestedZ, int activeX, int activeZ) {
        return new NavigationGoalPlan(
                RouteGoal.xz(requestedX, requestedZ),
                RouteGoal.xz(activeX, activeZ),
                false,
                8.0);
    }
}
