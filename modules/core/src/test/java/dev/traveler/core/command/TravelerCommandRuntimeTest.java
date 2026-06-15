package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.world.block.BlockPosition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerCommandRuntimeTest {
    @Test
    void pathTestRepliesAndStoresDebugResultWithoutMinecraftBindings() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerCommandRuntime runtime =
                new TravelerCommandRuntime(debugState, new TravelerNavigationState(), () -> null);
        RecordingSource source = new RecordingSource(null);

        TravelerCommandResponse response = runtime.pathCommands().pathTest(source);

        assertEquals("path test status=FOUND nodes=2", response.message());
        assertEquals(List.of("path test status=FOUND nodes=2"), source.replies);
        assertTrue(debugState.latestResult().isPresent());
    }

    @Test
    void navigateStopClearsNavigationStateAndDebugNavigation() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TravelerCommandRuntime runtime = new TravelerCommandRuntime(debugState, navigationState, () -> null);
        RecordingSource source = new RecordingSource(new BlockPosition(0, 64, 0));

        TravelerCommandResponse response = runtime.navigateCommands().stop(source);

        assertEquals("navigation stopped | debug nav cleared", response.message());
        assertEquals(List.of("navigation stopped | debug nav cleared"), source.replies);
        assertTrue(navigationState.activeSession().isEmpty());
        assertEquals("navigation stopped", navigationState.latestMessage().orElseThrow());
    }

    private static final class RecordingSource implements TravelerCommandSource {
        private final BlockPosition blockPosition;
        private final List<String> replies = new ArrayList<>();

        private RecordingSource(BlockPosition blockPosition) {
            this.blockPosition = blockPosition;
        }

        @Override
        public BlockPosition blockPosition() {
            return blockPosition;
        }

        @Override
        public void reply(String message) {
            replies.add(message);
        }
    }
}
