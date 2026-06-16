package dev.traveler.core.navigation.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.NavigationGoalPlan;
import dev.traveler.core.navigation.NavigationReplanActivation;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.navigation.api.NavigationSnapshot;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteGoal;
import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationSnapshotTest {
    @Test
    void snapshotReflectsStateTransitionsAndPreservesCompatibilityViews() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationPath activePath = navigationPath(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(4.0, 64.0, 0.0));
        NavigationGoalPlan activePlan = goalPlan(100, 0, 4, 0);

        state.start(activePath, "active", activePlan);

        NavigationSnapshot started = state.snapshot();
        assertEquals("active", started.latestMessage().orElseThrow());
        assertTrue(started.active().isPresent());
        assertFalse(started.prepared().isPresent());
        assertFalse(started.pending().isPresent());
        assertSame(state.activeSession().orElseThrow(), started.active().orElseThrow());

        NavigationPath lookaheadPath = navigationPath(
                new NavigationPoint(4.0, 64.0, 0.0),
                new NavigationPoint(8.0, 64.0, 0.0));
        NavigationGoalPlan lookaheadPlan = goalPlan(100, 0, 8, 0);
        state.prepareLookahead(lookaheadPath, "lookahead", lookaheadPlan);

        NavigationSnapshot prepared = state.snapshot();
        assertEquals("lookahead", prepared.latestMessage().orElseThrow());
        assertEquals(activePath, prepared.active().orElseThrow().path());
        assertEquals(lookaheadPath, prepared.prepared().orElseThrow().path());
        assertTrue(state.preparedLookaheadSession().isPresent());
        assertSame(state.preparedLookaheadSession().orElseThrow(), prepared.prepared().orElseThrow());

        NavigationPoint replanStart = new NavigationPoint(2.0, 64.0, 1.0);
        state.requestSegmentRepair(activePlan, "repair", replanStart);

        NavigationSnapshot requested = state.snapshot();
        assertEquals("repair", requested.latestMessage().orElseThrow());
        assertEquals(activePath, requested.active().orElseThrow().path());
        assertTrue(requested.prepared().isPresent());
        assertFalse(requested.pending().isPresent());
        assertEquals("repair", state.latestMessage().orElseThrow());

        TravelerNavigationState requestingState = new TravelerNavigationState();
        requestingState.start(activePath, "active", activePlan);
        requestingState.requestLookaheadReplan(activePlan, "lookahead requested", replanStart);

        NavigationSnapshot pending = requestingState.snapshot();
        assertEquals("lookahead requested", pending.latestMessage().orElseThrow());
        assertTrue(pending.active().isPresent());
        assertFalse(pending.prepared().isPresent());
        assertTrue(pending.pending().isPresent());
        assertEquals(RouteGoal.xz(100, 0), pending.pending().orElseThrow().goal());
        assertEquals(
                NavigationReplanActivation.PREPARE_LOOKAHEAD,
                pending.pending().orElseThrow().activation());
        assertEquals(replanStart, pending.pending().orElseThrow().startOverride().orElseThrow());
        assertSame(
                requestingState.pendingReplanRequest().orElseThrow(),
                pending.pending().orElseThrow());
    }

    private static NavigationPath navigationPath(NavigationPoint start, NavigationPoint end) {
        return NavigationPath.of(List.of(start, end));
    }

    private static NavigationGoalPlan goalPlan(int requestedX, int requestedZ, int activeX, int activeZ) {
        return new NavigationGoalPlan(
                RouteGoal.xz(requestedX, requestedZ),
                RouteGoal.xz(activeX, activeZ),
                false,
                8.0);
    }
}
