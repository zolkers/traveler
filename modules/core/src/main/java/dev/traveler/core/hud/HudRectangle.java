package dev.traveler.core.hud;

public record HudRectangle(int x, int y, int width, int height) {
    public HudRectangle {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Rectangle size must be non-negative.");
        }
    }
}
