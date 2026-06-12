package dev.traveler.core.command;

import java.util.Objects;

public record TravelerCommandRoute(String path, String description, TravelerCommandHandler handler) {
    public TravelerCommandRoute {
        path = requireText(path, "path");
        description = requireText(description, "description");
        handler = Objects.requireNonNull(handler, "handler");
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
