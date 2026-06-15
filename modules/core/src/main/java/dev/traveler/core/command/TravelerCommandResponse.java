package dev.traveler.core.command;

import java.util.Objects;

public record TravelerCommandResponse(String message) {
    public TravelerCommandResponse {
        Objects.requireNonNull(message, "message");
    }

    public static TravelerCommandResponse reply(TravelerCommandSource source, String message) {
        Objects.requireNonNull(source, "source").reply(message);
        return new TravelerCommandResponse(message);
    }
}
