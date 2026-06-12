package dev.traveler.mc.v1_21_11.common.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.BlockPosition;
import org.junit.jupiter.api.Test;

class PathfinderDebugStateTest {
    @Test
    void startsEmptyAndStoresLatestPathfinderResultWithMessageAndTimestamp() {
        PathfinderDebugState state = new PathfinderDebugState();
        PathfinderResult<BlockPosition> result =
                new PathfinderResult<>(PathfinderStatus.FOUND, new MutableGraphPath<>());

        assertFalse(state.latestResult().isPresent());
        assertFalse(state.latestMessage().isPresent());
        assertFalse(state.latestSnapshot().isPresent());
        assertFalse(state.updatedAt().isPresent());

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

        assertFalse(state.latestResult().isPresent());
        assertFalse(state.latestMessage().isPresent());
        assertFalse(state.latestSnapshot().isPresent());
        assertFalse(state.updatedAt().isPresent());
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
}
