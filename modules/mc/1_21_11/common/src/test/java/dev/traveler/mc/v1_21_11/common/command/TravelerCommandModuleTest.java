package dev.traveler.mc.v1_21_11.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerCommandModuleTest {
    @Test
    void registersPathTestCommandAndUpdatesDebugState() {
        TravelerCommandModule module = new TravelerCommandModule();
        TestSource source = new TestSource();

        CommandResult result = module.framework().dispatch(source, "traveler path test");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(module.debugState().latestResult().isPresent());
        assertTrue(module.debugState().latestMessage().orElseThrow().contains("path test"));
        assertEquals(List.of(result.reply().orElseThrow()), source.replies());
    }

    @Test
    void registersPathBlockCommandAndIncludesTargetInDebugMessage() {
        TravelerCommandModule module = new TravelerCommandModule();

        CommandResult result = module.framework().dispatch(new TestSource(), "traveler path block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(module.debugState().latestResult().isPresent());
        assertTrue(module.debugState().latestMessage().orElseThrow().contains("1,2,3"));
    }

    private static final class TestSource implements CommandSource {
        private final List<String> replies = new ArrayList<>();

        private List<String> replies() {
            return List.copyOf(replies);
        }

        @Override
        public void reply(String message) {
            replies.add(message);
        }
    }
}
