package dev.traveler.core.hud;

import java.util.Objects;

public record HudBox(String id, HudRectangle bounds, HudColor color) {
    public HudBox {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("id must not be blank.");
        }
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(color, "color");
    }
}
