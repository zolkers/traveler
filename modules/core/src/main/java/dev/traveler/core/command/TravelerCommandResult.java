package dev.traveler.core.command;

import java.util.Objects;
import java.util.Optional;

public record TravelerCommandResult(Status status, Optional<String> message) {
    public TravelerCommandResult {
        status = Objects.requireNonNull(status, "status");
        message = Objects.requireNonNull(message, "message");
    }

    public static TravelerCommandResult success(String message) {
        return new TravelerCommandResult(Status.SUCCESS, Optional.of(requireMessage(message)));
    }

    public static TravelerCommandResult failure(String message) {
        return new TravelerCommandResult(Status.FAILURE, Optional.of(requireMessage(message)));
    }

    public static TravelerCommandResult silentSuccess() {
        return new TravelerCommandResult(Status.SUCCESS, Optional.empty());
    }

    private static String requireMessage(String message) {
        String text = Objects.requireNonNull(message, "message").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return text;
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
