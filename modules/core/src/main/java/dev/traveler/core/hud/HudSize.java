package dev.traveler.core.hud;

public record HudSize(int width, int height) {
    public HudSize {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size must be non-negative.");
        }
    }
}
