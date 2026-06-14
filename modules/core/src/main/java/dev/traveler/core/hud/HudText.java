package dev.traveler.core.hud;

import java.util.Objects;

public record HudText(String id, String value, HudPoint position, HudColor color) {
    public HudText {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("id must not be blank.");
        }
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(color, "color");
    }
}
