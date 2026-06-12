package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TravelerCommandResultTest {
    @Test
    void successCarriesMessage() {
        TravelerCommandResult result = TravelerCommandResult.success("ok");

        assertEquals(TravelerCommandResult.Status.SUCCESS, result.status());
        assertEquals("ok", result.message().orElseThrow());
    }

    @Test
    void failureCarriesMessage() {
        TravelerCommandResult result = TravelerCommandResult.failure("nope");

        assertEquals(TravelerCommandResult.Status.FAILURE, result.status());
        assertEquals("nope", result.message().orElseThrow());
    }

    @Test
    void silentSuccessHasNoMessage() {
        TravelerCommandResult result = TravelerCommandResult.silentSuccess();

        assertEquals(TravelerCommandResult.Status.SUCCESS, result.status());
        assertTrue(result.message().isEmpty());
    }
}
