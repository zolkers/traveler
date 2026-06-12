package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
}
