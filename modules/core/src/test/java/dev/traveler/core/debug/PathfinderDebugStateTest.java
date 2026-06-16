package dev.traveler.core.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.debug.snapshots.NavigationDebugSnapshot;
import dev.traveler.core.debug.snapshots.PathfinderDebugSnapshot;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.testing.NavigationDebugFrames;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.block.BlockPosition;
import org.junit.jupiter.api.Test;

class PathfinderDebugStateTest {
    @Test
    void startsEmptyAndStoresLatestPathfinderResultWithMessageAndTimestamp() {
        PathfinderDebugState state = new PathfinderDebugState();
        PathfinderResult<BlockPosition> result =
                new PathfinderResult<>(PathfinderStatus.FOUND, new MutableGraphPath<>());

        assertEmpty(state);

        state.update(result, "path checked");

        PathfinderDebugSnapshot snapshot = state.latestSnapshot().orElseThrow();
        assertEquals(PathfinderStatus.FOUND, snapshot.result().status());
        assertSame(snapshot.result(), state.latestResult().orElseThrow());
        assertEquals("path checked", snapshot.message());
        assertEquals("path checked", state.latestMessage().orElseThrow());
        assertSame(snapshot.updatedAt(), state.updatedAt().orElseThrow());
    }

    @Test
    void canStoreResultWithoutMessageAndClearIt() {
        PathfinderDebugState state = new PathfinderDebugState();
        PathfinderResult<BlockPosition> result =
                new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>());

        state.update(result);
        state.clear();

        assertEmpty(state);
    }

    @Test
    void updateCopiesMutablePathIntoSnapshot() {
        PathfinderDebugState state = new PathfinderDebugState();
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        BlockPosition origin = new BlockPosition(1, 2, 3);
        path.addNode(origin);
        path.setCost(7.0);
        PathfinderResult<BlockPosition> result = new PathfinderResult<>(PathfinderStatus.FOUND, path);

        state.update(result, "copied");
        path.addNode(new BlockPosition(4, 5, 6));
        path.setCost(42.0);

        PathfinderResult<BlockPosition> stored = state.latestSnapshot().orElseThrow().result();
        assertEquals(PathfinderStatus.FOUND, stored.status());
        assertEquals(1, stored.path().nodeCount());
        assertEquals(origin, stored.path().nodeAt(0));
        assertEquals(7.0, stored.path().cost());
        assertThrows(UnsupportedOperationException.class, () -> stored.path().nodes().add(origin));
    }

    @Test
    void storesLatestNavigationSnapshotAndClearsItWithDebugState() {
        PathfinderDebugState state = new PathfinderDebugState();

        state.updateNavigation(frameInput(), frame());

        NavigationDebugSnapshot snapshot = state.latestNavigation().orElseThrow();
        assertEquals(NavigationPhase.APPROACH, snapshot.phase());
        assertEquals("nav phase=APPROACH", state.navigationSummary().orElseThrow().substring(0, 18));

        state.clearNavigation();
        assertFalse(state.latestNavigation().isPresent());
    }

    private static void assertEmpty(PathfinderDebugState state) {
        assertFalse(state.latestResult().isPresent());
        assertFalse(state.latestMessage().isPresent());
        assertFalse(state.latestSnapshot().isPresent());
        assertFalse(state.latestNavigation().isPresent());
        assertFalse(state.updatedAt().isPresent());
    }

    private static NavigationFrameInput frameInput() {
        return new NavigationFrameInput(
                new WorldPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);
    }

    private static NavigationControlFrame frame() {
        return NavigationDebugFrames.approachFrame(new WorldPoint(0.0, 64.0, 4.0));
    }
}
