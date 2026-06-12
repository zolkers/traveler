package dev.traveler.mc.v1_21_11.common.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertFalse(state.updatedAt().isPresent());

        state.update(result, "path checked");

        assertSame(result, state.latestResult().orElseThrow());
        assertEquals("path checked", state.latestMessage().orElseThrow());
        assertTrue(state.updatedAt().isPresent());
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
        assertFalse(state.updatedAt().isPresent());
    }
}
