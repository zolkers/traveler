package dev.traveler.core.navigation.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.navigation.NavigationGoalPlan;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.navigation.api.NavigationSnapshot;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.navigation.api.TraversalKind;
import dev.traveler.core.route.api.RoutePlan;
import dev.traveler.core.route.api.RouteSegment;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationSnapshotTest {
    private static final String INTERNAL_SESSION = "dev.traveler.core.navigation.NavigationSession";
    private static final String INTERNAL_REPLAN_REQUEST = "dev.traveler.core.navigation.NavigationReplanRequest";
    private static final String INTERNAL_GOAL_PLAN = "dev.traveler.core.navigation.NavigationGoalPlan";

    @Test
    void navigationSnapshotPublicApiDoesNotMentionInternalNavigationTypes() {
        for (Class<?> apiType : publicSnapshotTypes()) {
            for (Method method : apiType.getMethods()) {
                assertFalse(method.toGenericString().contains(INTERNAL_SESSION));
                assertFalse(method.toGenericString().contains(INTERNAL_REPLAN_REQUEST));
                assertFalse(method.toGenericString().contains(INTERNAL_GOAL_PLAN));
            }
            for (Constructor<?> constructor : apiType.getConstructors()) {
                assertFalse(constructor.toGenericString().contains(INTERNAL_SESSION));
                assertFalse(constructor.toGenericString().contains(INTERNAL_REPLAN_REQUEST));
                assertFalse(constructor.toGenericString().contains(INTERNAL_GOAL_PLAN));
            }
        }
    }

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
        assertEquals(
                List.of(
                        new NavigationPoint(0.0, 64.0, 0.0),
                        new NavigationPoint(4.0, 64.0, 0.0)),
                started.active().orElseThrow().nodes());
        assertEquals("active", started.active().orElseThrow().message());
        assertEquals(RouteGoal.xz(100, 0), started.active().orElseThrow().goalPlan().orElseThrow().requestedGoal());
        assertEquals(RouteGoal.xz(4, 0), started.active().orElseThrow().goalPlan().orElseThrow().activeGoal());

        NavigationPath lookaheadPath = navigationPath(
                new NavigationPoint(4.0, 64.0, 0.0),
                new NavigationPoint(8.0, 64.0, 0.0));
        NavigationGoalPlan lookaheadPlan = goalPlan(100, 0, 8, 0);
        state.prepareLookahead(lookaheadPath, "lookahead", lookaheadPlan);

        NavigationSnapshot prepared = state.snapshot();
        assertEquals("lookahead", prepared.latestMessage().orElseThrow());
        assertEquals(activePath.nodes(), prepared.active().orElseThrow().nodes());
        assertEquals(lookaheadPath.nodes(), prepared.prepared().orElseThrow().nodes());
        assertTrue(state.preparedLookaheadSession().isPresent());
        assertEquals("lookahead", prepared.prepared().orElseThrow().message());

        NavigationPoint replanStart = new NavigationPoint(2.0, 64.0, 1.0);
        state.requestSegmentRepair(activePlan, "repair", replanStart);

        NavigationSnapshot requested = state.snapshot();
        assertEquals("repair", requested.latestMessage().orElseThrow());
        assertEquals(activePath.nodes(), requested.active().orElseThrow().nodes());
        assertTrue(requested.prepared().isPresent());
        assertFalse(requested.pending().isPresent());
        assertEquals("repair", state.latestMessage().orElseThrow());
        assertEquals("active", started.latestMessage().orElseThrow());
        assertTrue(started.active().isPresent());
        assertFalse(started.prepared().isPresent());
        assertFalse(started.pending().isPresent());
        assertEquals(activePath.nodes(), started.active().orElseThrow().nodes());

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
                NavigationSnapshot.ReplanActivation.PREPARE_LOOKAHEAD,
                pending.pending().orElseThrow().activation());
        assertEquals("lookahead requested", pending.pending().orElseThrow().reason());
        assertEquals(replanStart, pending.pending().orElseThrow().startOverride().orElseThrow());
        assertFalse(pending.pending().orElseThrow().goalPlanOverride().isPresent());

        requestingState.stop("stopped");

        assertEquals("lookahead requested", pending.latestMessage().orElseThrow());
        assertTrue(pending.active().isPresent());
        assertFalse(pending.prepared().isPresent());
        assertTrue(pending.pending().isPresent());
        assertEquals(activePath.nodes(), pending.active().orElseThrow().nodes());
    }

    @Test
    void travelerNavigationStateRejectsNullMessagesAcrossTransitions() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationPath path = navigationPath(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(4.0, 64.0, 0.0));
        NavigationGoalPlan plan = goalPlan(100, 0, 4, 0);
        NavigationPoint startOverride = new NavigationPoint(2.0, 64.0, 1.0);

        assertThrows(NullPointerException.class, () -> state.start(path, null));
        assertThrows(NullPointerException.class, () -> state.start(path, null, plan));
        assertThrows(NullPointerException.class, () -> state.stop(null));
        assertThrows(NullPointerException.class, () -> state.requestReplan(plan, null));
        assertThrows(NullPointerException.class, () -> state.requestReplan(plan, null, startOverride));
        assertThrows(NullPointerException.class, () -> state.requestSegmentRepair(plan, null, startOverride));
        assertThrows(NullPointerException.class, () -> state.requestLookaheadReplan(plan, null));
        assertThrows(NullPointerException.class, () -> state.requestLookaheadReplan(plan, null, startOverride));
        assertThrows(NullPointerException.class, () -> state.prepareLookahead(path, null, plan));
        assertThrows(NullPointerException.class, () -> state.replaceActiveSession(path, null, plan));

        state.start(path, "active", plan);
        assertThrows(NullPointerException.class, () -> state.prepareLookahead(path, null, plan));
        assertThrows(NullPointerException.class, () -> state.replaceActiveSession(path, null, plan));
    }

    @Test
    void requestSegmentRepairSnapshotUsesReplaceActiveSessionBranch() {
        TravelerNavigationState state = new TravelerNavigationState();
        NavigationPath activePath = navigationPath(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(4.0, 64.0, 0.0));
        NavigationGoalPlan activePlan = goalPlan(100, 0, 4, 0);
        NavigationPoint repairStart = new NavigationPoint(2.0, 64.0, 1.0);

        state.start(activePath, "active", activePlan);
        state.requestSegmentRepair(activePlan, "repair", repairStart);

        NavigationSnapshot snapshot = state.snapshot();

        assertTrue(snapshot.pending().isPresent());
        assertEquals(
                NavigationSnapshot.ReplanActivation.REPLACE_ACTIVE_SESSION,
                snapshot.pending().orElseThrow().activation());
        assertEquals(RouteGoal.xz(4, 0), snapshot.pending().orElseThrow().goal());
        assertEquals(repairStart, snapshot.pending().orElseThrow().startOverride().orElseThrow());
        assertTrue(snapshot.pending().orElseThrow().goalPlanOverride().isPresent());
        assertEquals(
                RouteGoal.xz(100, 0),
                snapshot.pending().orElseThrow().goalPlanOverride().orElseThrow().requestedGoal());
        assertEquals(
                RouteGoal.xz(4, 0),
                snapshot.pending().orElseThrow().goalPlanOverride().orElseThrow().activeGoal());
    }

    @Test
    void routePlanRequiresSegmentIndexesToMatchListOrder() {
        RouteSegment first = new RouteSegment(0, new BlockPosition(0, 64, 0), new BlockPosition(1, 64, 0), TraversalKind.WALK);
        RouteSegment second = new RouteSegment(1, new BlockPosition(1, 64, 0), new BlockPosition(2, 64, 0), TraversalKind.WALK);

        RoutePlan plan = new RoutePlan(List.of(first, second));

        assertEquals(List.of(first, second), plan.segments());
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutePlan(List.of(
                        first,
                        new RouteSegment(2, new BlockPosition(1, 64, 0), new BlockPosition(2, 64, 0), TraversalKind.WALK))));
    }

    private static List<Class<?>> publicSnapshotTypes() {
        List<Class<?>> nestedPublicTypes = java.util.Arrays.stream(NavigationSnapshot.class.getDeclaredClasses())
                .filter(type -> Modifier.isPublic(type.getModifiers()))
                .toList();
        java.util.ArrayList<Class<?>> apiTypes = new java.util.ArrayList<>();
        apiTypes.add(NavigationSnapshot.class);
        apiTypes.addAll(nestedPublicTypes);
        return List.copyOf(apiTypes);
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
