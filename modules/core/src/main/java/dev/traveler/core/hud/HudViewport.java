package dev.traveler.core.hud;

public record HudViewport(int width, int height) {
    public HudViewport {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Viewport size must be non-negative.");
        }
    }
}
