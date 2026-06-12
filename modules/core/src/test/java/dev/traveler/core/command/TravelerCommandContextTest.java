package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TravelerCommandContextTest {
    @Test
    void readsTypedArguments() {
        TravelerCommandContext context = new TravelerCommandContext(
                Map.of("x", 12),
                message -> {
                });

        assertEquals(12, context.arg("x", int.class));
    }

    @Test
    void rejectsMissingArguments() {
        TravelerCommandContext context = new TravelerCommandContext(
                Map.of(),
                message -> {
                });

        assertThrows(IllegalArgumentException.class, () -> context.arg("x", int.class));
    }

    @Test
    void exposesFeedback() {
        List<String> replies = new ArrayList<>();
        TravelerCommandContext context = new TravelerCommandContext(Map.of(), replies::add);

        context.feedback().reply("hello");

        assertEquals(List.of("hello"), replies);
    }

    @Test
    void exposesTypedSource() {
        SourceProbe probe = new SourceProbe("player");
        TravelerCommandContext context = new TravelerCommandContext(Map.of(), message -> {}, new TestSource(probe));

        assertEquals(probe, context.source().unwrap(SourceProbe.class).orElseThrow());
        assertTrue(context.source().unwrap(String.class).isEmpty());
    }

    private record SourceProbe(String name) {
    }

    private record TestSource(SourceProbe probe) implements TravelerCommandSource {
        @Override
        public <T> Optional<T> unwrap(Class<T> type) {
            if (!type.isInstance(probe)) {
                return Optional.empty();
            }
            return Optional.of(type.cast(probe));
        }
    }
}
