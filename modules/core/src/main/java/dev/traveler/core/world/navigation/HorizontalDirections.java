package dev.traveler.core.world.navigation;

final class HorizontalDirections {
    static final HorizontalOffset[] EIGHT_WAY = {
            new HorizontalOffset(1, 0),
            new HorizontalOffset(-1, 0),
            new HorizontalOffset(0, 1),
            new HorizontalOffset(0, -1),
            new HorizontalOffset(1, 1),
            new HorizontalOffset(1, -1),
            new HorizontalOffset(-1, 1),
            new HorizontalOffset(-1, -1)
    };

    private HorizontalDirections() {}
}
